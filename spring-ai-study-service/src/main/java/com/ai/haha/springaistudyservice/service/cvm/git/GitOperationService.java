package com.ai.haha.springaistudyservice.service.cvm.git;

import com.ai.haha.springaistudyservice.service.cvm.dto.GitMergeResult;
import com.ai.haha.springaistudyservice.service.cvm.entity.CvmProject;

/**
 * Git操作服务接口。
 *
 * <p>为支持「git地址暂时留空也能完整跑通流程」，提供两种实现：</p>
 * <ul>
 *     <li>{@link SimulatedGitService}：模拟实现，仅登记分支与模拟合并/冲突，用于接入真实仓库前试用；</li>
 *     <li>{@link JGitGitService}：基于 JGit 的真实实现，执行真实的创建分支、合并、冲突检测、重置并推送远程。</li>
 * </ul>
 */
public interface GitOperationService {

    /**
     * 判断远程是否存在指定分支
     */
    boolean checkBranchExists(CvmProject project, String branch);

    /**
     * 创建分支（基于 baseBranch），若远程不存在则推送到远程
     */
    void createBranch(CvmProject project, String branch, String baseBranch);

    /**
     * 从远程已有分支拉取到本地（分支必须远程存在）
     */
    void pullBranch(CvmProject project, String branch);

    /**
     * 将 sourceBranch 合并到 targetBranch，返回合并结果（含冲突）
     */
    GitMergeResult mergeBranch(CvmProject project, String sourceBranch, String targetBranch);

    /**
     * 冲突已由用户在本地解决后，重新执行合并
     * （模拟实现会先将分支基线刷新到最新，避免再次误判冲突）
     */
    GitMergeResult resolveConflictAndMerge(CvmProject project, String sourceBranch, String targetBranch);

    /**
     * 将 targetBranch 重置到 sourceBranch 指向的提交（用于「基于master更新dev」）
     */
    void updateBranchFrom(CvmProject project, String targetBranch, String sourceBranch);

    /**
     * 无冲突检测合并：用于「进入正式环境」批量合入 release、以及「合并master后重建环境」时
     * 将既有已集成分支重新合入重建后的环境分支（这些分支本就应在目标分支上，强制合并即可）。
     * 默认实现走 {@link #resolveConflictAndMerge}（真实 git 合并不带冲突检测，冲突会返回冲突结果）。
     */
    default GitMergeResult mergeNoConflict(CvmProject project, String sourceBranch, String targetBranch) {
        return resolveConflictAndMerge(project, sourceBranch, targetBranch);
    }
}