package com.ai.haha.springaistudyservice.service.cvm.git;

import com.ai.haha.springaistudyservice.service.cvm.dto.GitMergeResult;
import com.ai.haha.springaistudyservice.service.cvm.entity.CvmProject;
import jakarta.annotation.Resource;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeCommand;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.RefSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 基于 JGit 的真实 Git 实现。
 *
 * <p>项目配置了 git 地址时使用。在本地工作区维护一份克隆，执行真实的
 * 创建分支、拉取、合并、冲突检测、重置并推送到远程仓库。</p>
 */
@Service
public class JGitGitService implements GitOperationService {

    private static final Logger log = LoggerFactory.getLogger(JGitGitService.class);

    @Resource
    private GitRepoManager repoManager;

    @Override
    public boolean checkBranchExists(CvmProject project, String branch) {
        try {
            Map<String, Ref> refs = lsRemoteWithRetry(project);
            return refs.containsKey("refs/heads/" + branch);
        } catch (Exception e) {
            log.warn("查询远程分支失败, url={}, branch={}, error={}", project.getGitUrl(), branch, e.getMessage());
            return false;
        }
    }

    @Override
    public void createBranch(CvmProject project, String branch, String baseBranch) {
        synchronized (repoManager.getLock(project.getProjectId())) {
            try (Git git = repoManager.openOrClone(project)) {
                fetchWithRetry(git, project);
                Ref baseRef = git.getRepository().findRef("refs/remotes/origin/" + baseBranch);
                if (baseRef == null) {
                    baseRef = git.getRepository().findRef("refs/heads/" + baseBranch);
                }
                if (baseRef == null) {
                    throw new IllegalStateException("找不到基础分支：" + baseBranch);
                }
                git.branchCreate().setName(branch).setStartPoint(baseRef.getName()).call();
                pushWithRetry(git, project, branch, false);
                log.info("创建并推送分支成功：{} -> {}", branch, project.getGitUrl());
            } catch (Exception e) {
                throw new RuntimeException("创建分支失败：" + branch + "，" + e.getMessage(), e);
            }
        }
    }

    @Override
    public void pullBranch(CvmProject project, String branch) {
        synchronized (repoManager.getLock(project.getProjectId())) {
            try (Git git = repoManager.openOrClone(project)) {
                fetchWithRetry(git, project);
                checkoutTracking(git, branch);
                log.info("拉取分支成功：{}", branch);
            } catch (Exception e) {
                throw new RuntimeException("拉取分支失败：" + branch + "，" + e.getMessage(), e);
            }
        }
    }

    @Override
    public GitMergeResult mergeBranch(CvmProject project, String sourceBranch, String targetBranch) {
        synchronized (repoManager.getLock(project.getProjectId())) {
            try (Git git = repoManager.openOrClone(project)) {
                fetchWithRetry(git, project);
                checkoutTracking(git, targetBranch);
                // 本地目标分支对齐远程最新，避免合并到过期分支
                git.reset().setRef("refs/remotes/origin/" + targetBranch)
                        .setMode(ResetCommand.ResetType.HARD).call();

                Ref sourceRef = git.getRepository().findRef("refs/remotes/origin/" + sourceBranch);
                if (sourceRef == null) {
                    sourceRef = git.getRepository().findRef("refs/heads/" + sourceBranch);
                }
                if (sourceRef == null) {
                    return GitMergeResult.fail("找不到源分支：" + sourceBranch);
                }

                MergeResult result = git.merge()
                        .include(sourceRef)
                        .setStrategy(MergeStrategy.RESOLVE)
                        .setFastForward(MergeCommand.FastForwardMode.FF)
                        .call();

                MergeResult.MergeStatus status = result.getMergeStatus();
                if (status.isSuccessful()) {
                    ObjectId head = result.getNewHead();
                    String commitId = head != null ? head.getName() : "unknown";
                    pushWithRetry(git, project, targetBranch, false);
                    return GitMergeResult.ok(commitId, "合并成功，已推送到远程");
                }
                if (status == MergeResult.MergeStatus.CONFLICTING) {
                    Map<String, int[][]> conflicts = result.getConflicts();
                    List<String> files = conflicts != null
                            ? new ArrayList<>(conflicts.keySet())
                            : Collections.emptyList();
                    git.reset().setMode(ResetCommand.ResetType.HARD).call();
                    return GitMergeResult.conflict(files,
                            "合并出现冲突，冲突文件：" + String.join(", ", files) + "，请按解决步骤处理后重新合并。");
                }
                git.reset().setMode(ResetCommand.ResetType.HARD).call();
                return GitMergeResult.fail("合并失败：" + status);
            } catch (Exception e) {
                throw new RuntimeException("合并失败：" + sourceBranch + " -> " + targetBranch + "，" + e.getMessage(), e);
            }
        }
    }

    @Override
    public GitMergeResult resolveConflictAndMerge(CvmProject project, String sourceBranch, String targetBranch) {
        // 冲突已由用户在本地解决并推送，重新合并即可
        return mergeBranch(project, sourceBranch, targetBranch);
    }

    @Override
    public void updateBranchFrom(CvmProject project, String targetBranch, String sourceBranch) {
        synchronized (repoManager.getLock(project.getProjectId())) {
            try (Git git = repoManager.openOrClone(project)) {
                fetchWithRetry(git, project);
                checkoutTracking(git, targetBranch);
                git.reset().setRef("refs/remotes/origin/" + sourceBranch)
                        .setMode(ResetCommand.ResetType.HARD).call();
                pushWithRetry(git, project, targetBranch, true);
                log.info("环境重置成功：{} 已基于 {} 强推", targetBranch, sourceBranch);
            } catch (Exception e) {
                throw new RuntimeException("环境重置失败：" + targetBranch + " <- " + sourceBranch + "，" + e.getMessage(), e);
            }
        }
    }

    @Override
    public void exitIntegration(CvmProject project, String sourceBranch, String targetBranch, String mergeCommit) {
        if (mergeCommit == null || mergeCommit.isBlank()) {
            throw new RuntimeException("缺少合并提交号，无法执行物理回滚");
        }
        synchronized (repoManager.getLock(project.getProjectId())) {
            try (Git git = repoManager.openOrClone(project)) {
                Repository repo = git.getRepository();
                fetchWithRetry(git, project);
                checkoutTracking(git, targetBranch);
                git.reset().setRef("refs/remotes/origin/" + targetBranch)
                        .setMode(ResetCommand.ResetType.HARD).call();

                ObjectId id = repo.resolve(mergeCommit);
                if (id == null) {
                    throw new RuntimeException("找不到合并提交：" + mergeCommit);
                }
                try (RevWalk walk = new RevWalk(repo)) {
                    RevCommit merge = walk.parseCommit(id);
                    RevCommit[] parents = merge.getParents();
                    if (parents.length < 2) {
                        // 非 merge 提交，直接 revert
                        git.revert().include(merge).call();
                    } else {
                        // merge 提交：以第一个父提交为主分支，生成反向补丁并应用（等价于 git revert -m 1）
                        RevCommit mainline = walk.parseCommit(parents[0]);
                        revertMergeByReverseDiff(git, mainline, merge);
                    }
                }
                pushWithRetry(git, project, targetBranch, false);
                log.info("退出集成成功：revert {}（{} -> {}）", mergeCommit, sourceBranch, targetBranch);
            } catch (Exception e) {
                throw new RuntimeException("物理回滚失败：" + e.getMessage(), e);
            }
        }
    }

    /**
     * 反向应用 merge 提交相对主分支引入的差异，生成一个回滚提交（JGit RevertCommand 不支持 -m 主分支，故手动实现）
     */
    private void revertMergeByReverseDiff(Git git, RevCommit mainline, RevCommit merge) throws Exception {
        Repository repo = git.getRepository();
        ByteArrayOutputStream patch = new ByteArrayOutputStream();
        try (DiffFormatter df = new DiffFormatter(patch)) {
            df.setRepository(repo);
            List<DiffEntry> entries = df.scan(mainline.getTree(), merge.getTree());
            df.format(entries);
        }
        // 反向补丁（反转每条 diff 的 + / -，以及新增/删除文件头）
        String reversed = reversePatch(new String(patch.toByteArray(), StandardCharsets.UTF_8));
        try {
            git.apply().setPatch(new ByteArrayInputStream(reversed.getBytes(StandardCharsets.UTF_8))).call();
        } catch (org.eclipse.jgit.api.errors.PatchApplyException e) {
            log.warn("反向补丁未能干净应用（可能存在后续其他合并，跳过冲突部分）：{}", e.getMessage());
        }
        git.commit().setMessage("revert: 退出集成 " + merge.name()).call();
    }

    /**
     * 将 git diff 输出中的新增行/删除行互换，得到反向补丁
     */
    private String reversePatch(String diff) {
        StringBuilder sb = new StringBuilder();
        for (String line : diff.split("\n", -1)) {
            if (line.startsWith("new file mode")) {
                sb.append(line.replaceFirst("^new file mode", "deleted file mode")).append("\n");
            } else if (line.startsWith("deleted file mode")) {
                sb.append(line.replaceFirst("^deleted file mode", "new file mode")).append("\n");
            } else if (line.startsWith("+") && !line.startsWith("+++")) {
                sb.append("-").append(line.substring(1)).append("\n");
            } else if (line.startsWith("-") && !line.startsWith("---")) {
                sb.append("+").append(line.substring(1)).append("\n");
            } else {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * 检出分支（本地存在则直接检出，否则基于远程创建跟踪分支）
     */
    private void checkoutTracking(Git git, String branch) throws Exception {
        Repository repo = git.getRepository();
        boolean localExists = repo.findRef("refs/heads/" + branch) != null;
        if (localExists) {
            git.checkout().setName(branch).call();
        } else {
            git.checkout().setName(branch)
                    .setCreateBranch(true)
                    .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK)
                    .setStartPoint("refs/remotes/origin/" + branch)
                    .call();
        }
    }

    private CredentialsProvider credentials(CvmProject project) {
        return repoManager.buildCredentials(project);
    }

    /**
     * 带重试的 fetch（部分网络下 TLS 握手会间歇性被重置）
     */
    private void fetchWithRetry(Git git, CvmProject project) {
        repoManager.withGitRetry("fetch", () -> {
            try {
                git.fetch().setRemote("origin").setCredentialsProvider(credentials(project)).call();
                return null;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * 带重试的 push
     */
    private void pushWithRetry(Git git, CvmProject project, String branch, boolean force) {
        repoManager.withGitRetry("push", () -> {
            try {
                git.push().setRemote("origin")
                        .setRefSpecs(new RefSpec("refs/heads/" + branch))
                        .setForce(force)
                        .setCredentialsProvider(credentials(project))
                        .call();
                return null;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * 带重试的 ls-remote（查询远程分支）
     */
    private Map<String, Ref> lsRemoteWithRetry(CvmProject project) {
        return repoManager.withGitRetry("ls-remote", () -> {
            try {
                return Git.lsRemoteRepository()
                        .setRemote(project.getGitUrl())
                        .setCredentialsProvider(credentials(project))
                        .callAsMap();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
}
