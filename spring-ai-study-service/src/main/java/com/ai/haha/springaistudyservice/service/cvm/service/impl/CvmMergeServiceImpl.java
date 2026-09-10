package com.ai.haha.springaistudyservice.service.cvm.service.impl;

import cn.hutool.json.JSONUtil;
import com.ai.haha.springaistudyservice.service.cvm.dto.CvmMergeRecordView;
import com.ai.haha.springaistudyservice.service.cvm.dto.GitMergeResult;
import com.ai.haha.springaistudyservice.service.cvm.entity.CvmCrRecord;
import com.ai.haha.springaistudyservice.service.cvm.entity.CvmMergeRecord;
import com.ai.haha.springaistudyservice.service.cvm.entity.CvmProject;
import com.ai.haha.springaistudyservice.service.cvm.entity.CvmRequirement;
import com.ai.haha.springaistudyservice.service.cvm.entity.CvmUser;
import com.ai.haha.springaistudyservice.service.cvm.enums.EnvCode;
import com.ai.haha.springaistudyservice.service.cvm.enums.MergeStatus;
import com.ai.haha.springaistudyservice.service.cvm.enums.OperationAction;
import com.ai.haha.springaistudyservice.service.cvm.enums.RequirementStatus;
import com.ai.haha.springaistudyservice.service.cvm.git.GitConflictGuide;
import com.ai.haha.springaistudyservice.service.cvm.git.GitOperationService;
import com.ai.haha.springaistudyservice.service.cvm.git.GitServiceFactory;
import com.ai.haha.springaistudyservice.service.cvm.mapper.CvmCrRecordMapper;
import com.ai.haha.springaistudyservice.service.cvm.mapper.CvmMergeRecordMapper;
import com.ai.haha.springaistudyservice.service.cvm.mapper.CvmProjectMapper;
import com.ai.haha.springaistudyservice.service.cvm.mapper.CvmRequirementMapper;
import com.ai.haha.springaistudyservice.service.cvm.mapper.CvmUserMapper;
import com.ai.haha.springaistudyservice.service.cvm.service.CvmMergeService;
import com.ai.haha.springaistudyservice.service.cvm.service.CvmOperationLogService;
import com.ai.haha.springaistudyservice.service.moyoo.util.SnowflakeIdGenerator;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * CVM合并服务实现
 */
@Service
public class CvmMergeServiceImpl implements CvmMergeService {

    private static final Logger log = LoggerFactory.getLogger(CvmMergeServiceImpl.class);

    @Resource
    private CvmMergeRecordMapper mergeRecordMapper;

    @Resource
    private CvmRequirementMapper requirementMapper;

    @Resource
    private CvmProjectMapper projectMapper;

    @Resource
    private CvmUserMapper cvmUserMapper;

    @Resource
    private CvmCrRecordMapper crRecordMapper;

    @Resource
    private GitServiceFactory gitServiceFactory;

    @Resource
    private CvmOperationLogService operationLogService;

    @Resource
    private SnowflakeIdGenerator idGenerator;

    @Override
    @Transactional
    public CvmMergeRecordView merge(Long mergeId, Long operatorUserId) {
        CvmMergeRecord record = mustGetMergeRecord(mergeId);
        if (!MergeStatus.PENDING.getCode().equals(record.getStatus())) {
            throw new RuntimeException("当前记录状态为「" + mergeStatusDesc(record.getStatus()) + "」，不可合并");
        }
        CvmProject project = projectMapper.selectByProjectId(record.getProjectId());
        if (project == null) {
            throw new RuntimeException("项目不存在");
        }
        CvmRequirement requirement = requirementMapper.selectByRequirementId(record.getRequirementId());
        if (requirement == null) {
            throw new RuntimeException("需求不存在");
        }
        if (RequirementStatus.RELEASED.getCode().equals(requirement.getStatus())) {
            throw new RuntimeException("该分支已上线（正式环境），不可再集成到环境分支");
        }
        if (EnvCode.PREVIEW.getCode().equals(record.getTargetEnv()) && !crRecordMapper.hasPassedByRequirement(record.getRequirementId())) {
            throw new RuntimeException("预发环境集成前需先完成该分支的CR审核");
        }

        GitOperationService git = gitServiceFactory.getService(project);
        GitMergeResult result = git.mergeBranch(project, record.getBranchName(), record.getTargetBranch());
        if (result.isSuccess()) {
            record.setStatus(MergeStatus.MERGED.getCode());
            record.setMergeCommit(result.getCommitId());
            record.setMergeTime(LocalDateTime.now());
            record.setUpdateTime(LocalDateTime.now());
            mergeRecordMapper.updateById(record);
            updateRequirementAfterMerge(requirement);
            operationLogService.record(project.getProjectId(), record.getRequirementId(), mergeId, operatorUserId,
                    OperationAction.MERGE,
                    "分支 " + record.getBranchName() + " 已合并到 " + record.getTargetBranch()
                            + "（commit: " + result.getCommitId() + "）");
        } else {
            record.setStatus(MergeStatus.CONFLICT.getCode());
            record.setConflictFiles(result.getConflictFiles() != null ? JSONUtil.toJsonStr(result.getConflictFiles()) : null);
            record.setConflictDetail(result.getMessage());
            record.setResolveSteps(GitConflictGuide.buildResolveSteps(record.getBranchName(), record.getTargetBranch()));
            record.setUpdateTime(LocalDateTime.now());
            mergeRecordMapper.updateById(record);
            updateRequirementStatus(record.getRequirementId(), RequirementStatus.CONFLICT);
            operationLogService.record(project.getProjectId(), record.getRequirementId(), mergeId, operatorUserId,
                    OperationAction.MERGE_CONFLICT,
                    "分支 " + record.getBranchName() + " 合并到 " + record.getTargetBranch() + " 出现冲突");
        }
        return toView(record);
    }

    @Override
    @Transactional
    public CvmMergeRecordView resolveConflict(Long mergeId, Long operatorUserId) {
        CvmMergeRecord record = mustGetMergeRecord(mergeId);
        if (!MergeStatus.CONFLICT.getCode().equals(record.getStatus())) {
            throw new RuntimeException("当前记录不存在待解决的冲突，无法重新合并");
        }
        CvmProject project = projectMapper.selectByProjectId(record.getProjectId());
        if (project == null) {
            throw new RuntimeException("项目不存在");
        }
        GitOperationService git = gitServiceFactory.getService(project);
        GitMergeResult result = git.resolveConflictAndMerge(project, record.getBranchName(), record.getTargetBranch());
        if (result.isSuccess()) {
            record.setStatus(MergeStatus.MERGED.getCode());
            record.setMergeCommit(result.getCommitId());
            record.setMergeTime(LocalDateTime.now());
            record.setConflictFiles(null);
            record.setConflictDetail(null);
            record.setResolveSteps(null);
            record.setUpdateTime(LocalDateTime.now());
            mergeRecordMapper.updateById(record);
            updateRequirementStatus(record.getRequirementId(), RequirementStatus.MERGED);
            operationLogService.record(project.getProjectId(), record.getRequirementId(), mergeId, operatorUserId,
                    OperationAction.RESOLVE_CONFLICT,
                    "冲突已解决，" + record.getBranchName() + " 已合并到 " + record.getTargetBranch()
                            + "（commit: " + result.getCommitId() + "）");
        } else {
            throw new RuntimeException("重新合并仍存在冲突：" + result.getMessage());
        }
        return toView(record);
    }

    @Override
    @Transactional
    public void exitIntegration(Long mergeId, Long operatorUserId) {
        CvmMergeRecord record = mustGetMergeRecord(mergeId);
        if (!MergeStatus.MERGED.getCode().equals(record.getStatus())
                && !MergeStatus.CONFLICT.getCode().equals(record.getStatus())) {
            throw new RuntimeException("只有已集成或冲突待解决的分支才能退出集成");
        }
        CvmProject project = projectMapper.selectByProjectId(record.getProjectId());
        if (project == null) {
            throw new RuntimeException("项目不存在");
        }
        CvmRequirement requirement = requirementMapper.selectByRequirementId(record.getRequirementId());
        if (requirement == null) {
            throw new RuntimeException("需求不存在");
        }
        if (RequirementStatus.RELEASED.getCode().equals(requirement.getStatus())) {
            throw new RuntimeException("该分支已上线（正式环境），不可退出集成");
        }

        // 真实 git 物理回滚（尽力而为，不影响流程状态）；冲突记录无合并提交号，跳过物理回滚
        GitOperationService git = gitServiceFactory.getService(project);
        if (StringUtils.hasText(record.getMergeCommit())) {
            try {
                git.exitIntegration(project, record.getBranchName(), record.getTargetBranch(), record.getMergeCommit());
            } catch (Exception e) {
                log.warn("退出集成物理回滚失败（不影响流程状态）：{}", e.getMessage());
            }
        }

        record.setStatus(MergeStatus.PENDING.getCode());
        record.setMergeCommit(null);
        record.setMergeTime(null);
        record.setConflictFiles(null);
        record.setConflictDetail(null);
        record.setResolveSteps(null);
        record.setUpdateTime(LocalDateTime.now());
        mergeRecordMapper.updateById(record);

        // 若该需求在其他环境仍有已合并记录，则保持「已合并」；否则回到「开发中」
        EnvCode highest = highestMergedEnv(requirement);
        if (highest != null) {
            requirement.setStatus(RequirementStatus.MERGED.getCode());
            requirement.setCurrentEnv(highest.getCode());
        } else {
            requirement.setStatus(RequirementStatus.DEVELOPING.getCode());
            requirement.setCurrentEnv(EnvCode.DEV.getCode());
        }
        requirement.setUpdateTime(LocalDateTime.now());
        requirementMapper.updateById(requirement);

        operationLogService.record(project.getProjectId(), requirement.getRequirementId(), mergeId, operatorUserId,
                OperationAction.EXIT_INTEGRATION,
                "分支 " + record.getBranchName() + " 已从 " + record.getTargetBranch() + " 退出集成，回到待集成列表");
    }

    @Override
    @Transactional
    public int enterRelease(Long projectId, Long operatorUserId) {
        CvmProject project = projectMapper.selectByProjectId(projectId);
        if (project == null) {
            throw new RuntimeException("项目不存在");
        }
        // 预发环境已集成的全部分支（尚未进入正式环境的）
        List<CvmMergeRecord> toEnter = new ArrayList<>();
        for (CvmMergeRecord record : mergeRecordMapper.selectByProjectIdAndEnv(projectId, EnvCode.PREVIEW.getCode())) {
            if (!MergeStatus.MERGED.getCode().equals(record.getStatus())) {
                continue;
            }
            CvmRequirement requirement = requirementMapper.selectByRequirementId(record.getRequirementId());
            if (requirement == null || RequirementStatus.RELEASED.getCode().equals(requirement.getStatus())) {
                continue;
            }
            toEnter.add(record);
        }
        if (toEnter.isEmpty()) {
            throw new RuntimeException("预发环境暂无需要进入正式环境的分支");
        }
        // 卡点：全部通过 CR 审核才能进入正式环境
        List<String> needCr = new ArrayList<>();
        for (CvmMergeRecord record : toEnter) {
            if (!crRecordMapper.hasPassedByRequirement(record.getRequirementId())) {
                needCr.add(record.getBranchName());
            }
        }
        if (!needCr.isEmpty()) {
            throw new RuntimeException("以下分支尚未完成 CR 审核，无法进入正式环境：" + String.join("、", needCr));
        }

        GitOperationService git = gitServiceFactory.getService(project);
        int entered = 0;
        List<String> failures = new ArrayList<>();
        for (CvmMergeRecord record : toEnter) {
            try {
                GitMergeResult result = git.mergeNoConflict(project, record.getBranchName(), EnvCode.RELEASE.getBranchName());
                if (!result.isSuccess()) {
                    failures.add(record.getBranchName() + "：" + result.getMessage());
                    continue;
                }
                CvmMergeRecord releaseRecord = new CvmMergeRecord();
                releaseRecord.setMergeId(idGenerator.nextId());
                releaseRecord.setProjectId(projectId);
                releaseRecord.setRequirementId(record.getRequirementId());
                releaseRecord.setRequirementName(record.getRequirementName());
                releaseRecord.setBranchName(record.getBranchName());
                releaseRecord.setUserId(operatorUserId);
                releaseRecord.setTargetEnv(EnvCode.RELEASE.getCode());
                releaseRecord.setTargetBranch(EnvCode.RELEASE.getBranchName());
                releaseRecord.setStatus(MergeStatus.MERGED.getCode());
                releaseRecord.setCrRequired(false);
                releaseRecord.setMergeCommit(result.getCommitId());
                releaseRecord.setMergeTime(LocalDateTime.now());
                releaseRecord.setCreateTime(LocalDateTime.now());
                releaseRecord.setUpdateTime(LocalDateTime.now());
                mergeRecordMapper.insert(releaseRecord);

                CvmRequirement requirement = requirementMapper.selectByRequirementId(record.getRequirementId());
                if (requirement != null) {
                    requirement.setStatus(RequirementStatus.RELEASED.getCode());
                    requirement.setCurrentEnv(EnvCode.RELEASE.getCode());
                    requirement.setUpdateTime(LocalDateTime.now());
                    requirementMapper.updateById(requirement);
                }
                operationLogService.record(projectId, record.getRequirementId(), releaseRecord.getMergeId(), operatorUserId,
                        OperationAction.ENTER_RELEASE,
                        "分支 " + record.getBranchName() + " 已进入正式环境，合并到 release（commit: " + result.getCommitId() + "）");
                entered++;
            } catch (Exception e) {
                failures.add(record.getBranchName() + "：" + e.getMessage());
            }
        }
        if (entered == 0) {
            throw new RuntimeException("进入正式环境失败：" + String.join("；", failures));
        }
        if (!failures.isEmpty()) {
            log.warn("进入正式环境部分失败：{}", failures);
        }
        return entered;
    }

    @Override
    @Transactional
    public int mergeMaster(Long projectId, Long operatorUserId) {
        CvmProject project = projectMapper.selectByProjectId(projectId);
        if (project == null) {
            throw new RuntimeException("项目不存在");
        }
        List<CvmMergeRecord> released = new ArrayList<>();
        for (CvmMergeRecord record : mergeRecordMapper.selectByProjectIdAndEnv(projectId, EnvCode.RELEASE.getCode())) {
            if (MergeStatus.MERGED.getCode().equals(record.getStatus())) {
                released.add(record);
            }
        }
        if (released.isEmpty()) {
            throw new RuntimeException("正式环境暂无上线分支，无法合并 master");
        }
        Set<Long> releasedReqIds = new HashSet<>();
        for (CvmMergeRecord record : released) {
            releasedReqIds.add(record.getRequirementId());
        }

        GitOperationService git = gitServiceFactory.getService(project);

        // 1. 上线分支合并到 master
        for (CvmMergeRecord record : released) {
            GitMergeResult result = git.mergeBranch(project, record.getBranchName(), "master");
            if (!result.isSuccess()) {
                String msg = "上线分支 " + record.getBranchName() + " 合并 master 失败";
                if (result.getConflictFiles() != null && !result.getConflictFiles().isEmpty()) {
                    msg += "，冲突文件：" + String.join(", ", result.getConflictFiles())
                            + "\n" + GitConflictGuide.buildResolveSteps(record.getBranchName(), "master")
                            + "\n解决完成后重新点击【合并 master】";
                }
                throw new RuntimeException(msg + "：" + result.getMessage());
            }
            operationLogService.record(projectId, record.getRequirementId(), null, operatorUserId,
                    OperationAction.MERGE_MASTER,
                    "上线分支 " + record.getBranchName() + " 已合并到 master（commit: " + result.getCommitId() + "）");
        }

        // 2. 重建环境分支：dev/test/preview/release 对齐 master，并将仍存续的已集成分支重新合入
        for (EnvCode envCode : EnvCode.values()) {
            try {
                rebuildEnvBranch(project, envCode, releasedReqIds, operatorUserId);
            } catch (Exception e) {
                log.warn("环境重建失败：{}，原因：{}", envCode.getBranchName(), e.getMessage());
            }
        }

        // 3. 逻辑删除上线分支及其需求、合并记录、CR记录（远程 git 分支保留，不删除）
        for (CvmMergeRecord record : released) {
            logicalDeleteRequirement(record.getRequirementId());
        }
        operationLogService.record(projectId, null, null, operatorUserId,
                OperationAction.MERGE_MASTER,
                "正式环境 " + released.size() + " 个上线分支已合并 master，相关需求已归档（逻辑删除），各环境分支已重建并重新合并存续分支");
        return released.size();
    }

    @Override
    public List<CvmMergeRecordView> listByEnv(Long projectId, String envCode) {
        // 开发/测试/预发三个环境的待集成列表展示项目全部分支；正式环境仅展示已上线分支
        if (!EnvCode.RELEASE.getCode().equals(envCode)) {
            ensureEnvRecords(projectId, envCode);
        }
        List<CvmMergeRecordView> views = new ArrayList<>();
        for (CvmMergeRecord record : mergeRecordMapper.selectByProjectIdAndEnv(projectId, envCode)) {
            views.add(toView(record));
        }
        return views;
    }

    @Override
    public List<CvmMergeRecordView> listAllByProject(Long projectId) {
        List<CvmMergeRecordView> views = new ArrayList<>();
        for (EnvCode envCode : EnvCode.values()) {
            views.addAll(listByEnv(projectId, envCode.getCode()));
        }
        return views;
    }

    // ======================= 内部工具方法 =======================

    private CvmMergeRecord mustGetMergeRecord(Long mergeId) {
        CvmMergeRecord record = mergeRecordMapper.selectByMergeId(mergeId);
        if (record == null) {
            throw new RuntimeException("合并记录不存在");
        }
        return record;
    }

    private boolean hasMergedInEnv(CvmRequirement requirement, EnvCode envCode) {
        for (CvmMergeRecord record : mergeRecordMapper.selectByProjectIdAndEnv(requirement.getProjectId(), envCode.getCode())) {
            if (requirement.getRequirementId().equals(record.getRequirementId())
                    && MergeStatus.MERGED.getCode().equals(record.getStatus())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 合并成功后更新需求状态与「当前环境」（取已合并环境中排序最高者）
     */
    private void updateRequirementAfterMerge(CvmRequirement requirement) {
        requirement.setStatus(RequirementStatus.MERGED.getCode());
        EnvCode highest = highestMergedEnv(requirement);
        if (highest != null) {
            requirement.setCurrentEnv(highest.getCode());
        }
        requirement.setUpdateTime(LocalDateTime.now());
        requirementMapper.updateById(requirement);
    }

    /**
     * 需求当前已合并到的排序最高的环境（不含 release，release 由进入正式环境单独维护）
     */
    private EnvCode highestMergedEnv(CvmRequirement requirement) {
        EnvCode highest = null;
        for (EnvCode envCode : EnvCode.values()) {
            if (envCode == EnvCode.RELEASE) {
                continue;
            }
            if (hasMergedInEnv(requirement, envCode)) {
                if (highest == null || envCode.getSortOrder() > highest.getSortOrder()) {
                    highest = envCode;
                }
            }
        }
        return highest;
    }

    /**
     * 确保该环境为项目下每个未上线需求都生成了待合并记录（三个环境都展示项目全部分支）
     */
    private void ensureEnvRecords(Long projectId, String envCode) {
        EnvCode env = EnvCode.fromCode(envCode);
        if (env == null) {
            return;
        }
        List<CvmRequirement> requirements = requirementMapper.selectByProjectId(projectId);
        List<CvmMergeRecord> existing = mergeRecordMapper.selectByProjectIdAndEnv(projectId, envCode);
        for (CvmRequirement requirement : requirements) {
            if (RequirementStatus.RELEASED.getCode().equals(requirement.getStatus())) {
                continue; // 已上线需求不再补充环境记录
            }
            boolean exists = false;
            for (CvmMergeRecord record : existing) {
                if (requirement.getRequirementId().equals(record.getRequirementId())) {
                    exists = true;
                    break;
                }
            }
            if (exists) {
                continue;
            }
            CvmMergeRecord record = new CvmMergeRecord();
            record.setMergeId(idGenerator.nextId());
            record.setProjectId(projectId);
            record.setRequirementId(requirement.getRequirementId());
            record.setRequirementName(requirement.getRequirementName());
            record.setBranchName(requirement.getBranchName());
            record.setUserId(requirement.getCreatorUserId());
            record.setTargetEnv(envCode);
            record.setTargetBranch(env.getBranchName());
            record.setStatus(MergeStatus.PENDING.getCode());
            record.setCrRequired(env == EnvCode.PREVIEW);
            record.setCreateTime(LocalDateTime.now());
            record.setUpdateTime(LocalDateTime.now());
            mergeRecordMapper.insert(record);
        }
    }

    /**
     * 重建单个环境分支：对齐 master，并将该环境仍存续的已集成分支重新合并（本次已上线分支不再合并）
     */
    private void rebuildEnvBranch(CvmProject project, EnvCode envCode, Set<Long> releasedReqIds, Long operatorUserId) {
        GitOperationService git = gitServiceFactory.getService(project);
        git.updateBranchFrom(project, envCode.getBranchName(), "master");
        int remerged = 0;
        for (CvmMergeRecord record : mergeRecordMapper.selectByProjectIdAndEnv(project.getProjectId(), envCode.getCode())) {
            if (!MergeStatus.MERGED.getCode().equals(record.getStatus())) {
                continue;
            }
            if (releasedReqIds.contains(record.getRequirementId())) {
                continue; // 本次已上线的分支不再重新合并
            }
            GitMergeResult result = git.mergeNoConflict(project, record.getBranchName(), envCode.getBranchName());
            if (result.isSuccess()) {
                remerged++;
            } else {
                log.warn("环境 {} 重建后重新合并 {} 失败：{}", envCode.getBranchName(), record.getBranchName(), result.getMessage());
            }
        }
        operationLogService.record(project.getProjectId(), null, null, operatorUserId,
                OperationAction.REBUILD_ENV,
                envCode.getDescription() + "分支（" + envCode.getBranchName() + "）已基于 master 重建，重新合并 " + remerged + " 个存续分支");
    }

    /**
     * 逻辑删除需求及其全部合并记录、CR记录（远程 git 分支保留）
     */
    private void logicalDeleteRequirement(Long requirementId) {
        CvmRequirement requirement = requirementMapper.selectByRequirementId(requirementId);
        if (requirement == null) {
            return;
        }
        requirementMapper.deleteById(requirement.getId());
        for (EnvCode envCode : EnvCode.values()) {
            for (CvmMergeRecord record : mergeRecordMapper.selectByProjectIdAndEnv(requirement.getProjectId(), envCode.getCode())) {
                if (requirementId.equals(record.getRequirementId())) {
                    mergeRecordMapper.deleteById(record.getId());
                    for (CvmCrRecord cr : crRecordMapper.selectByMergeId(record.getMergeId())) {
                        crRecordMapper.deleteById(cr.getId());
                    }
                }
            }
        }
    }

    private void updateRequirementStatus(Long requirementId, RequirementStatus status) {
        CvmRequirement requirement = requirementMapper.selectByRequirementId(requirementId);
        if (requirement != null) {
            requirement.setStatus(status.getCode());
            requirement.setUpdateTime(LocalDateTime.now());
            requirementMapper.updateById(requirement);
        }
    }

    private String mergeStatusDesc(String status) {
        MergeStatus mergeStatus = MergeStatus.fromCode(status);
        return mergeStatus != null ? mergeStatus.getDescription() : status;
    }

    private CvmMergeRecordView toView(CvmMergeRecord record) {
        CvmMergeRecordView view = new CvmMergeRecordView();
        view.setMergeId(record.getMergeId());
        view.setProjectId(record.getProjectId());
        view.setRequirementId(record.getRequirementId());
        view.setRequirementName(record.getRequirementName());
        view.setBranchName(record.getBranchName());
        view.setUserId(record.getUserId());
        view.setTargetEnv(record.getTargetEnv());
        view.setTargetBranch(record.getTargetBranch());
        view.setStatus(record.getStatus());
        view.setStatusDesc(mergeStatusDesc(record.getStatus()));
        view.setMergeCommit(record.getMergeCommit());
        view.setCrRequired(record.getCrRequired());
        view.setMergeTime(record.getMergeTime());
        view.setCreateTime(record.getCreateTime());
        view.setConflictDetail(record.getConflictDetail());
        view.setResolveSteps(record.getResolveSteps());
        if (StringUtils.hasText(record.getConflictFiles())) {
            try {
                view.setConflictFiles(JSONUtil.toList(record.getConflictFiles(), String.class));
            } catch (Exception e) {
                log.warn("解析冲突文件列表失败：{}", record.getConflictFiles(), e);
            }
        }
        CvmUser user = record.getUserId() != null ? cvmUserMapper.selectByUserId(record.getUserId()) : null;
        if (user != null) {
            view.setUserName(StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername());
        } else {
            view.setUserName("未知用户");
        }
        List<CvmCrRecord> crs = crRecordMapper.selectByRequirementId(record.getRequirementId());
        if (crs.isEmpty()) {
            view.setCrStatus("NONE");
        } else {
            view.setCrStatus(crs.get(crs.size() - 1).getCrStatus());
        }
        return view;
    }
}
