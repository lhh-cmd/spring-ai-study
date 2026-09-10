package com.ai.haha.springaistudyservice.service.cvm.git;

import com.ai.haha.springaistudyservice.service.cvm.dto.GitMergeResult;
import com.ai.haha.springaistudyservice.service.cvm.entity.CvmProject;
import jakarta.annotation.Resource;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeCommand;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.RefSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

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
            Map<String, Ref> refs = Git.lsRemoteRepository()
                    .setRemote(project.getGitUrl())
                    .setCredentialsProvider(credentials(project))
                    .callAsMap();
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
                git.fetch().setRemote("origin").setCredentialsProvider(credentials(project)).call();
                Ref baseRef = git.getRepository().findRef("refs/remotes/origin/" + baseBranch);
                if (baseRef == null) {
                    baseRef = git.getRepository().findRef("refs/heads/" + baseBranch);
                }
                if (baseRef == null) {
                    throw new IllegalStateException("找不到基础分支：" + baseBranch);
                }
                git.branchCreate().setName(branch).setStartPoint(baseRef.getName()).call();
                git.push().setRemote("origin")
                        .setRefSpecs(new RefSpec("refs/heads/" + branch))
                        .setCredentialsProvider(credentials(project))
                        .call();
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
                git.fetch().setRemote("origin").setCredentialsProvider(credentials(project)).call();
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
                git.fetch().setRemote("origin").setCredentialsProvider(credentials(project)).call();
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
                    git.push().setRemote("origin")
                            .setRefSpecs(new RefSpec("refs/heads/" + targetBranch))
                            .setCredentialsProvider(credentials(project))
                            .call();
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
                git.fetch().setRemote("origin").setCredentialsProvider(credentials(project)).call();
                checkoutTracking(git, targetBranch);
                git.reset().setRef("refs/remotes/origin/" + sourceBranch)
                        .setMode(ResetCommand.ResetType.HARD).call();
                git.push().setRemote("origin")
                        .setRefSpecs(new RefSpec("refs/heads/" + targetBranch))
                        .setForce(true)
                        .setCredentialsProvider(credentials(project))
                        .call();
                log.info("环境重置成功：{} 已基于 {} 强推", targetBranch, sourceBranch);
            } catch (Exception e) {
                throw new RuntimeException("环境重置失败：" + targetBranch + " <- " + sourceBranch + "，" + e.getMessage(), e);
            }
        }
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
}
