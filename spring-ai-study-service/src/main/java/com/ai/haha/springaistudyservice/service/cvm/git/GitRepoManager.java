package com.ai.haha.springaistudyservice.service.cvm.git;

import com.ai.haha.springaistudyservice.service.cvm.entity.CvmGitAccount;
import com.ai.haha.springaistudyservice.service.cvm.entity.CvmProject;
import com.ai.haha.springaistudyservice.service.cvm.mapper.CvmGitAccountMapper;
import jakarta.annotation.Resource;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Git 本地仓库管理器。
 *
 * <p>每个项目在本地工作区维护一份克隆（默认 ./cvm-workspace/{projectId}/{repoName}），
 * 并按项目加锁，避免同一仓库并发合并互相污染。</p>
 */
@Component
public class GitRepoManager {

    private static final Logger log = LoggerFactory.getLogger(GitRepoManager.class);

    @Value("${cvm.workspace.dir:./cvm-workspace}")
    private String workspaceDir;

    @Value("${cvm.git.username:}")
    private String globalUsername;

    @Value("${cvm.git.token:}")
    private String globalToken;

    @Resource
    private CvmGitAccountMapper gitAccountMapper;

    private final Map<Long, Object> projectLocks = new ConcurrentHashMap<>();

    /**
     * 获取项目级同步锁
     */
    public Object getLock(Long projectId) {
        return projectLocks.computeIfAbsent(projectId, k -> new Object());
    }

    /**
     * 解析启用的全局 Git 服务账号：优先 DB（cvm_git_account 表），其次环境变量/配置，未配置则抛异常。
     */
    private CvmGitAccount resolveAccount() {
        CvmGitAccount account = gitAccountMapper.selectActive();
        if (account != null && StringUtils.hasText(account.getGitToken())) {
            return account;
        }
        return null;
    }

    /**
     * 构建全局服务账号凭据。所有 git 操作统一使用该账号（DB cvm_git_account 表 或 环境变量 CVM_GIT_USERNAME/CVM_GIT_TOKEN），
     * 忽略项目级 gitUsername/gitToken，保证「注册只需 Git 地址、统一账号操作真实仓库」。
     * token 未配置时直接抛异常，避免真实模式匿名操作静默失败。
     */
    public CredentialsProvider buildCredentials(CvmProject project) {
        CvmGitAccount account = resolveAccount();
        String user = account != null ? account.getAccountName() : globalUsername;
        String token = account != null ? account.getGitToken() : globalToken;
        if (!StringUtils.hasText(user) || !StringUtils.hasText(token)) {
            throw new IllegalStateException("未配置全局 Git 服务账号：请在数据库 cvm_git_account 表配置账号（或设置环境变量 CVM_GIT_USERNAME / CVM_GIT_TOKEN）");
        }
        return new UsernamePasswordCredentialsProvider(user, token);
    }

    /**
     * 解析服务账号用户名（供提交身份使用）
     */
    private String resolveUsername() {
        CvmGitAccount account = resolveAccount();
        if (account != null && StringUtils.hasText(account.getAccountName())) {
            return account.getAccountName();
        }
        return StringUtils.hasText(globalUsername) ? globalUsername : "cvm-bot";
    }

    /**
     * 计算本地仓库目录
     */
    public File getRepoDir(CvmProject project) {
        String url = project.getGitUrl();
        String repoName = url;
        int idx = url.lastIndexOf('/');
        if (idx >= 0) {
            repoName = url.substring(idx + 1);
        }
        repoName = repoName.replaceAll("\\.git$", "");
        if (!StringUtils.hasText(repoName)) {
            repoName = "repo";
        }
        return Paths.get(workspaceDir, String.valueOf(project.getProjectId()), repoName).toFile();
    }

    /**
     * 打开或克隆本地仓库（调用方需自行关闭 Git 实例）。
     * 打开/克隆后均把仓库级提交身份设为全局服务账号，保证 merge/revert 提交作者为服务账号。
     */
    public Git openOrClone(CvmProject project) {
        synchronized (getLock(project.getProjectId())) {
            File dir = getRepoDir(project);
            if (dir.exists() && new File(dir, ".git").exists()) {
                try {
                    Git git = Git.open(dir);
                    configureIdentity(git);
                    return git;
                } catch (IOException e) {
                    throw new RuntimeException("打开本地仓库失败：" + dir.getAbsolutePath(), e);
                }
            }
            File parent = dir.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }
            Git git = withGitRetry("clone", () -> {
                try {
                    log.info("克隆仓库：{} -> {}", project.getGitUrl(), dir.getAbsolutePath());
                    return Git.cloneRepository()
                            .setURI(project.getGitUrl())
                            .setDirectory(dir)
                            .setCredentialsProvider(buildCredentials(project))
                            .setCloneAllBranches(true)
                            .call();
                } catch (GitAPIException e) {
                    throw new RuntimeException("克隆仓库失败：" + project.getGitUrl() + "，" + e.getMessage(), e);
                }
            });
            configureIdentity(git);
            return git;
        }
    }

    /**
     * 是否为可重试的瞬时网络/TLS 故障（代理或直连在部分网络下会间歇性 TLS 握手被重置）
     */
    private static boolean isTransientTls(Throwable e) {
        String msg = String.valueOf(e.getMessage());
        return msg != null && (msg.contains("SSL") || msg.contains("Secure connection")
                || msg.contains("shut down") || msg.contains("Connection reset")
                || msg.contains("reset by peer") || msg.contains("Read timed out")
                || msg.contains("connect timed out"));
    }

    /**
     * 带重试的网络操作：瞬时 TLS/网络抖动导致失败时最多重试 6 次。
     * 该环境（GFW）直连与代理两条到 github 的链路会随时间交替抖动，无法保证任一链路稳定，
     * 故用多次重试覆盖两个链路的间歇性失败。
     */
    public <T> T withGitRetry(String op, java.util.function.Supplier<T> action) {
        RuntimeException last = null;
        for (int attempt = 1; attempt <= 6; attempt++) {
            try {
                return action.get();
            } catch (RuntimeException e) {
                last = e;
                if (isTransientTls(e)) {
                    if (attempt < 6) {
                        log.warn("[{}] 第 {} 次瞬时网络/TLS失败，{}ms 后重试：{}", op, attempt, attempt * 1200, e.getMessage());
                        try {
                            Thread.sleep(attempt * 1200L);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                } else {
                    throw e;
                }
            }
        }
        throw last;
    }

    /**
     * 将仓库级 user.name / user.email 设为全局服务账号（JGit 提交时读取仓库 config 作为作者/提交者）。
     * 配置失败不影响打开/克隆，仅记录日志。
     */
    private void configureIdentity(Git git) {
        try {
            String name = resolveUsername();
            StoredConfig config = git.getRepository().getConfig();
            config.setString("user", null, "name", name);
            config.setString("user", null, "email", name + "@users.noreply.github.com");
            config.save();
        } catch (IOException e) {
            log.warn("设置仓库提交身份失败，将使用默认身份：{}", e.getMessage());
        }
    }
}
