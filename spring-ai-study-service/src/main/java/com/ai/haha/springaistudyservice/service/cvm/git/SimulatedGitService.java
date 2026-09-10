package com.ai.haha.springaistudyservice.service.cvm.git;

import cn.hutool.core.util.RandomUtil;
import com.ai.haha.springaistudyservice.service.cvm.dto.GitMergeResult;
import com.ai.haha.springaistudyservice.service.cvm.entity.CvmBranch;
import com.ai.haha.springaistudyservice.service.cvm.entity.CvmMergeRecord;
import com.ai.haha.springaistudyservice.service.cvm.entity.CvmProject;
import com.ai.haha.springaistudyservice.service.cvm.enums.BranchSource;
import com.ai.haha.springaistudyservice.service.cvm.enums.BranchType;
import com.ai.haha.springaistudyservice.service.cvm.enums.EnvCode;
import com.ai.haha.springaistudyservice.service.cvm.enums.MergeStatus;
import com.ai.haha.springaistudyservice.service.cvm.mapper.CvmBranchMapper;
import com.ai.haha.springaistudyservice.service.cvm.mapper.CvmMergeRecordMapper;
import com.ai.haha.springaistudyservice.service.moyoo.util.SnowflakeIdGenerator;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 模拟Git实现。
 *
 * <p>在项目未配置 git 地址时使用，保证系统无需真实仓库即可完整跑通全部流程：
 * 分支通过 {@link CvmBranch} 登记表维护「远程是否存在」，合并冲突通过目标环境分支的
 * 已合并计数启发式判定（分支创建后目标分支被其他分支合并过，则视为冲突）。</p>
 */
@Service
public class SimulatedGitService implements GitOperationService {

    private static final Logger log = LoggerFactory.getLogger(SimulatedGitService.class);

    @Resource
    private CvmBranchMapper branchMapper;

    @Resource
    private CvmMergeRecordMapper mergeRecordMapper;

    @Resource
    private SnowflakeIdGenerator idGenerator;

    @Override
    public boolean checkBranchExists(CvmProject project, String branch) {
        return branchMapper.selectByProjectIdAndBranchName(project.getProjectId(), branch) != null;
    }

    @Override
    public void createBranch(CvmProject project, String branch, String baseBranch) {
        registerBranch(project, branch, baseBranch, BranchSource.NEW);
    }

    @Override
    public void pullBranch(CvmProject project, String branch) {
        registerBranch(project, branch, "origin", BranchSource.REMOTE);
    }

    /**
     * master 目标分支在冲突启发式中使用的伪环境编码（master 不参与环境合并计数，故恒为 0）
     */
    private static final String MASTER_ENV = "MASTER";

    @Override
    public GitMergeResult mergeBranch(CvmProject project, String sourceBranch, String targetBranch) {
        boolean toMaster = isMaster(targetBranch);
        String targetEnv = toEnvCode(targetBranch);
        if (targetEnv == null && !toMaster) {
            return GitMergeResult.fail("未知的目标分支：" + targetBranch);
        }
        CvmBranch src = branchMapper.selectByProjectIdAndBranchName(project.getProjectId(), sourceBranch);
        if (src == null) {
            registerBranch(project, sourceBranch, targetBranch, BranchSource.NEW);
            src = branchMapper.selectByProjectIdAndBranchName(project.getProjectId(), sourceBranch);
        }

        String countEnv = toMaster ? MASTER_ENV : targetEnv;
        long baseCount = mergeRecordMapper.countMergedBefore(project.getProjectId(), countEnv, src.getCreateTime());
        long currentCount = mergeRecordMapper.countMerged(project.getProjectId(), countEnv);
        if (currentCount > baseCount) {
            List<String> otherBranches = new ArrayList<>();
            for (CvmMergeRecord r : mergeRecordMapper.selectByProjectIdAndEnv(project.getProjectId(), targetEnv)) {
                if (MergeStatus.MERGED.getCode().equals(r.getStatus())) {
                    otherBranches.add(r.getBranchName());
                }
            }
            return GitMergeResult.conflict(otherBranches,
                    "（模拟）检测到冲突：" + targetBranch + " 分支在您的分支 " + sourceBranch + " 创建之后，已被其他分支合并过，"
                            + "目标分支代码已前进。请将本分支合并到 " + targetBranch + " 上解决冲突（详见【解决冲突】弹窗中的步骤），"
                            + "解决并推送后点击【冲突已解决】按钮完成合并。");
        }

        String commitId = "sim-" + RandomUtil.randomString(40);
        log.info("模拟合并成功：{} -> {}, commit={}", sourceBranch, targetBranch, commitId);
        return GitMergeResult.ok(commitId, "模拟合并成功");
    }

    @Override
    public GitMergeResult resolveConflictAndMerge(CvmProject project, String sourceBranch, String targetBranch) {
        // 模拟实现：冲突已由用户在本地解决，先将分支基线刷新到最新，避免再次误判冲突
        CvmBranch src = branchMapper.selectByProjectIdAndBranchName(project.getProjectId(), sourceBranch);
        if (src != null) {
            src.setCreateTime(LocalDateTime.now());
            src.setUpdateTime(LocalDateTime.now());
            branchMapper.updateById(src);
        }
        return mergeBranch(project, sourceBranch, targetBranch);
    }

    @Override
    public GitMergeResult mergeNoConflict(CvmProject project, String sourceBranch, String targetBranch) {
        // 强制合并：登记分支并直接返回成功，不做冲突启发式判定（用于进入正式环境与重建环境分支）
        CvmBranch src = branchMapper.selectByProjectIdAndBranchName(project.getProjectId(), sourceBranch);
        if (src == null) {
            registerBranch(project, sourceBranch, targetBranch, BranchSource.NEW);
        }
        String commitId = "sim-" + RandomUtil.randomString(40);
        log.info("模拟强制合并成功：{} -> {}, commit={}", sourceBranch, targetBranch, commitId);
        return GitMergeResult.ok(commitId, "模拟合并成功");
    }

    @Override
    public void updateBranchFrom(CvmProject project, String targetBranch, String sourceBranch) {
        CvmBranch b = branchMapper.selectByProjectIdAndBranchName(project.getProjectId(), targetBranch);
        if (b != null) {
            b.setBaseBranch(sourceBranch);
            b.setUpdateTime(LocalDateTime.now());
            branchMapper.updateById(b);
        }
        log.info("模拟环境重置：{} 已基于 {} 更新", targetBranch, sourceBranch);
    }

    /**
     * 登记分支
     */
    private void registerBranch(CvmProject project, String branch, String baseBranch, BranchSource source) {
        if (branchMapper.selectByProjectIdAndBranchName(project.getProjectId(), branch) != null) {
            return;
        }
        CvmBranch b = new CvmBranch();
        b.setBranchId(idGenerator.nextId());
        b.setProjectId(project.getProjectId());
        b.setBranchName(branch);
        b.setBranchType(detectType(branch));
        b.setBaseBranch(baseBranch);
        b.setSource(source.getCode());
        b.setCreateTime(LocalDateTime.now());
        b.setUpdateTime(LocalDateTime.now());
        branchMapper.insert(b);
    }

    /**
     * 根据分支名推断分支类型
     */
    private String detectType(String branch) {
        if ("master".equals(branch) || "main".equals(branch)) {
            return BranchType.MASTER.getCode();
        }
        for (EnvCode envCode : EnvCode.values()) {
            if (envCode.getBranchName().equals(branch)) {
                return BranchType.ENV_BRANCH.getCode();
            }
        }
        return BranchType.REQUIREMENT_BRANCH.getCode();
    }

    /**
     * 是否为 master/main 主分支
     */
    private boolean isMaster(String branchName) {
        return "master".equals(branchName) || "main".equals(branchName);
    }

    /**
     * 分支名转环境编码
     */
    private String toEnvCode(String branchName) {
        for (EnvCode envCode : EnvCode.values()) {
            if (envCode.getBranchName().equals(branchName)) {
                return envCode.getCode();
            }
        }
        return null;
    }
}
