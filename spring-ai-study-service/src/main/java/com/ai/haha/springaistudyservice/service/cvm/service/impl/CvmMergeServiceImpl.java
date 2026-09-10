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
import java.util.List;

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
        // release 环境 CR 拦截
        if (Boolean.TRUE.equals(record.getCrRequired()) && !crRecordMapper.hasPassed(mergeId)) {
            throw new RuntimeException("该需求进入正式环境需先完成代码评审(CR)，请先在 CR 审核区通过后再合并");
        }

        GitOperationService git = gitServiceFactory.getService(project);
        GitMergeResult result = git.mergeBranch(project, record.getBranchName(), record.getTargetBranch());
        if (result.isSuccess()) {
            record.setStatus(MergeStatus.MERGED.getCode());
            record.setMergeCommit(result.getCommitId());
            record.setMergeTime(LocalDateTime.now());
            record.setUpdateTime(LocalDateTime.now());
            mergeRecordMapper.updateById(record);
            updateRequirementStatus(record.getRequirementId(), RequirementStatus.MERGED);
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
    public void nextEnv(Long requirementId, Long operatorUserId) {
        CvmRequirement requirement = requirementMapper.selectByRequirementId(requirementId);
        if (requirement == null) {
            throw new RuntimeException("需求不存在");
        }
        if (RequirementStatus.CONFLICT.getCode().equals(requirement.getStatus())) {
            throw new RuntimeException("当前需求存在未解决的冲突，请先解决冲突后再进入下一环境");
        }
        EnvCode current = EnvCode.fromCode(requirement.getCurrentEnv());
        if (current == null) {
            throw new RuntimeException("需求环境异常：" + requirement.getCurrentEnv());
        }
        EnvCode next = current.next();
        if (next == null) {
            throw new RuntimeException("已处于正式环境，请执行「发布」或「合并master」操作");
        }
        // 当前环境必须先合并成功
        if (!hasMergedInEnv(requirement, current)) {
            throw new RuntimeException("当前环境（" + current.getDescription() + "）的分支尚未合并成功，请先完成合并验证");
        }

        CvmMergeRecord record = new CvmMergeRecord();
        record.setMergeId(idGenerator.nextId());
        record.setProjectId(requirement.getProjectId());
        record.setRequirementId(requirement.getRequirementId());
        record.setRequirementName(requirement.getRequirementName());
        record.setBranchName(requirement.getBranchName());
        record.setUserId(operatorUserId);
        record.setTargetEnv(next.getCode());
        record.setTargetBranch(next.getBranchName());
        record.setStatus(MergeStatus.PENDING.getCode());
        record.setCrRequired(next == EnvCode.RELEASE);
        record.setCreateTime(LocalDateTime.now());
        record.setUpdateTime(LocalDateTime.now());
        mergeRecordMapper.insert(record);

        requirement.setCurrentEnv(next.getCode());
        requirement.setStatus(RequirementStatus.DEVELOPING.getCode());
        requirement.setUpdateTime(LocalDateTime.now());
        requirementMapper.updateById(requirement);

        operationLogService.record(requirement.getProjectId(), requirement.getRequirementId(), record.getMergeId(),
                operatorUserId, OperationAction.NEXT_ENV,
                "需求 " + requirement.getRequirementName() + " 已进入" + next.getDescription() + "待合并列表"
                        + (next == EnvCode.RELEASE ? "（该环境合并前需通过 CR 审核）" : ""));
    }

    @Override
    @Transactional
    public void publish(Long requirementId, Long operatorUserId) {
        CvmRequirement requirement = requirementMapper.selectByRequirementId(requirementId);
        if (requirement == null) {
            throw new RuntimeException("需求不存在");
        }
        if (!EnvCode.RELEASE.getCode().equals(requirement.getCurrentEnv())) {
            throw new RuntimeException("只有正式环境的需求才能发布到线上");
        }
        if (!hasMergedInEnv(requirement, EnvCode.RELEASE)) {
            throw new RuntimeException("正式环境尚未合并，无法发布");
        }
        requirement.setStatus(RequirementStatus.PUBLISHED.getCode());
        requirement.setUpdateTime(LocalDateTime.now());
        requirementMapper.updateById(requirement);
        operationLogService.record(requirement.getProjectId(), requirement.getRequirementId(), null, operatorUserId,
                OperationAction.PUBLISH,
                "需求 " + requirement.getRequirementName() + " 已发布到线上系统");
    }

    @Override
    @Transactional
    public void mergeToMaster(Long requirementId, Long operatorUserId) {
        CvmRequirement requirement = requirementMapper.selectByRequirementId(requirementId);
        if (requirement == null) {
            throw new RuntimeException("需求不存在");
        }
        if (!EnvCode.RELEASE.getCode().equals(requirement.getCurrentEnv())) {
            throw new RuntimeException("只有正式环境的需求才能合并 master");
        }
        String status = requirement.getStatus();
        if (!RequirementStatus.MERGED.getCode().equals(status) && !RequirementStatus.PUBLISHED.getCode().equals(status)) {
            throw new RuntimeException("需求需先完成正式环境合并（或已发布）后才能合并 master");
        }
        CvmProject project = projectMapper.selectByProjectId(requirement.getProjectId());
        if (project == null) {
            throw new RuntimeException("项目不存在");
        }
        GitOperationService git = gitServiceFactory.getService(project);

        // 1. 分支合并到 master
        GitMergeResult result = git.mergeBranch(project, requirement.getBranchName(), "master");
        if (!result.isSuccess()) {
            if (result.getConflictFiles() != null && !result.getConflictFiles().isEmpty()) {
                throw new RuntimeException("合并 master 出现冲突，冲突文件："
                        + String.join(", ", result.getConflictFiles())
                        + "\n请先在本地方解决冲突：\n" + GitConflictGuide.buildResolveSteps(requirement.getBranchName(), "master")
                        + "\n解决完成后重新点击【合并master】");
            }
            throw new RuntimeException("合并 master 失败：" + result.getMessage());
        }

        // 2. 其他环境退出已合并分支：基于 master 重置各环境公共分支
        for (EnvCode envCode : EnvCode.values()) {
            try {
                git.updateBranchFrom(project, envCode.getBranchName(), "master");
                operationLogService.record(project.getProjectId(), requirement.getRequirementId(), null, operatorUserId,
                        OperationAction.RESET_ENV,
                        envCode.getDescription() + "分支（" + envCode.getBranchName() + "）已基于 master 更新");
            } catch (Exception e) {
                log.warn("环境重置失败：{}，原因：{}", envCode.getBranchName(), e.getMessage());
            }
        }

        // 3. 将已合入 master 的分支重新合并到更新后的 dev（基于 master 的 dev 已包含该分支，合并为快速前进）
        try {
            GitMergeResult remerge = git.resolveConflictAndMerge(project, requirement.getBranchName(), "dev");
            operationLogService.record(project.getProjectId(), requirement.getRequirementId(), null, operatorUserId,
                    OperationAction.MERGE,
                    "已合 master 的分支 " + requirement.getBranchName() + " 已重新合并到更新后的 dev"
                            + (remerge.isSuccess() ? "（commit: " + remerge.getCommitId() + "）" : ""));
        } catch (Exception e) {
            log.warn("重新合并到 dev 失败：{}", e.getMessage());
        }

        // 4. 需求进入终态
        requirement.setStatus(RequirementStatus.MERGED_MASTER.getCode());
        requirement.setUpdateTime(LocalDateTime.now());
        requirementMapper.updateById(requirement);
        operationLogService.record(project.getProjectId(), requirement.getRequirementId(), null, operatorUserId,
                OperationAction.MERGE_MASTER,
                "需求 " + requirement.getRequirementName() + " 分支 " + requirement.getBranchName() + " 已合并到 master"
                        + "，其他环境已退出该分支并基于 master 更新 dev");
    }

    @Override
    public List<CvmMergeRecordView> listByEnv(Long projectId, String envCode) {
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
        if (Boolean.TRUE.equals(record.getCrRequired())) {
            List<CvmCrRecord> crs = crRecordMapper.selectByMergeId(record.getMergeId());
            if (crs.isEmpty()) {
                view.setCrStatus("PENDING");
            } else {
                view.setCrStatus(crs.get(crs.size() - 1).getCrStatus());
            }
        } else {
            view.setCrStatus("NONE");
        }
        return view;
    }
}
