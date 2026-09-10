package com.ai.haha.springaistudyservice.service.cvm.git;

import com.ai.haha.springaistudyservice.service.cvm.entity.CvmProject;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
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

    private final Map<Long, Object> projectLocks = new ConcurrentHashMap<>();

    /**
     * 获取项目级同步锁
     */
    public Object getLock(Long projectId) {
        return projectLocks.computeIfAbsent(projectId, k -> new Object());
    }

    /**
     * 构建凭据（优先项目配置，其次全局配置，无配置则匿名访问）
     */
    public CredentialsProvider buildCredentials(CvmProject project) {
        String user = StringUtils.hasText(project.getGitUsername()) ? project.getGitUsername() : globalUsername;
        String token = StringUtils.hasText(project.getGitToken()) ? project.getGitToken() : globalToken;
        if (StringUtils.hasText(user) && StringUtils.hasText(token)) {
            return new UsernamePasswordCredentialsProvider(user, token);
        }
        return null;
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
     * 打开或克隆本地仓库（调用方需自行关闭 Git 实例）
     */
    public Git openOrClone(CvmProject project) {
        synchronized (getLock(project.getProjectId())) {
            File dir = getRepoDir(project);
            if (dir.exists() && new File(dir, ".git").exists()) {
                try {
                    return Git.open(dir);
                } catch (IOException e) {
                    throw new RuntimeException("打开本地仓库失败：" + dir.getAbsolutePath(), e);
                }
            }
            File parent = dir.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }
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
        }
    }
}
