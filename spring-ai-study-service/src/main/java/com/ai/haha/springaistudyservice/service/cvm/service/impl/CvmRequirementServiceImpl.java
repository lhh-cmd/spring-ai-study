package com.ai.haha.springaistudyservice.service.cvm.service.impl;

import com.ai.haha.springaistudyservice.service.cvm.dto.CvmRequirementCreateDTO;
import com.ai.haha.springaistudyservice.service.cvm.entity.CvmMergeRecord;
import com.ai.haha.springaistudyservice.service.cvm.entity.CvmProject;
import com.ai.haha.springaistudyservice.service.cvm.entity.CvmRequirement;
import com.ai.haha.springaistudyservice.service.cvm.enums.BranchSource;
import com.ai.haha.springaistudyservice.service.cvm.enums.EnvCode;
import com.ai.haha.springaistudyservice.service.cvm.enums.MergeStatus;
import com.ai.haha.springaistudyservice.service.cvm.enums.OperationAction;
import com.ai.haha.springaistudyservice.service.cvm.enums.RequirementStatus;
import com.ai.haha.springaistudyservice.service.cvm.git.GitOperationService;
import com.ai.haha.springaistudyservice.service.cvm.git.GitServiceFactory;
import com.ai.haha.springaistudyservice.service.cvm.mapper.CvmMergeRecordMapper;
import com.ai.haha.springaistudyservice.service.cvm.mapper.CvmProjectMapper;
import com.ai.haha.springaistudyservice.service.cvm.mapper.CvmRequirementMapper;
import com.ai.haha.springaistudyservice.service.cvm.service.CvmOperationLogService;
import com.ai.haha.springaistudyservice.service.cvm.service.CvmRequirementService;
import com.ai.haha.springaistudyservice.service.moyoo.util.SnowflakeIdGenerator;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * CVM需求服务实现
 */
@Service
public class CvmRequirementServiceImpl implements CvmRequirementService {

    @Resource
    private CvmRequirementMapper requirementMapper;

    @Resource
    private CvmProjectMapper projectMapper;

    @Resource
    private CvmMergeRecordMapper mergeRecordMapper;

    @Resource
    private GitServiceFactory gitServiceFactory;

    @Resource
    private CvmOperationLogService operationLogService;

    @Resource
    private SnowflakeIdGenerator idGenerator;

    @Override
    @Transactional
    public CvmRequirement createRequirement(CvmRequirementCreateDTO dto) {
        if (dto.getProjectId() == null) {
            throw new RuntimeException("所属项目不能为空");
        }
        if (!StringUtils.hasText(dto.getRequirementName())) {
            throw new RuntimeException("需求名称不能为空");
        }
        if (!StringUtils.hasText(dto.getBranchName())) {
            throw new RuntimeException("分支名称不能为空");
        }
        CvmProject project = projectMapper.selectByProjectId(dto.getProjectId());
        if (project == null) {
            throw new RuntimeException("项目不存在");
        }
        if (dto.getCreatorUserId() == null) {
            throw new RuntimeException("创建人不能为空");
        }

        BranchSource source;
        try {
            source = StringUtils.hasText(dto.getBranchSource())
                    ? BranchSource.valueOf(dto.getBranchSource())
                    : BranchSource.NEW;
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("分支来源无效：" + dto.getBranchSource());
        }

        GitOperationService git = gitServiceFactory.getService(project);
        String devBranch = EnvCode.DEV.getBranchName();
        if (source == BranchSource.NEW) {
            if (git.checkBranchExists(project, dto.getBranchName())) {
                throw new RuntimeException("远程已存在同名分支：" + dto.getBranchName() + "，请选择「从远程已有拉取」");
            }
            git.createBranch(project, dto.getBranchName(), devBranch);
        } else {
            if (!git.checkBranchExists(project, dto.getBranchName())) {
                throw new RuntimeException("远程不存在该分支：" + dto.getBranchName() + "，请确认分支名或选择「新建」");
            }
            git.pullBranch(project, dto.getBranchName());
        }

        CvmRequirement requirement = new CvmRequirement();
        requirement.setRequirementId(idGenerator.nextId());
        requirement.setProjectId(project.getProjectId());
        requirement.setRequirementName(dto.getRequirementName());
        requirement.setRequirementUrl(dto.getRequirementUrl());
        requirement.setBranchName(dto.getBranchName());
        requirement.setBranchSource(source.getCode());
        requirement.setCreatorUserId(dto.getCreatorUserId());
        requirement.setCurrentEnv(EnvCode.DEV.getCode());
        requirement.setStatus(RequirementStatus.DEVELOPING.getCode());
        requirement.setCreateTime(LocalDateTime.now());
        requirement.setUpdateTime(LocalDateTime.now());
        requirementMapper.insert(requirement);

        // 生成 开发/测试/预发 三个环境的待合并记录（每个环境的待集成列表均展示项目全部分支）
        CvmMergeRecord devRecord = null;
        for (EnvCode envCode : EnvCode.values()) {
            if (envCode == EnvCode.RELEASE) {
                continue; // 正式环境记录仅在进入正式环境时创建
            }
            CvmMergeRecord record = new CvmMergeRecord();
            record.setMergeId(idGenerator.nextId());
            record.setProjectId(project.getProjectId());
            record.setRequirementId(requirement.getRequirementId());
            record.setRequirementName(requirement.getRequirementName());
            record.setBranchName(requirement.getBranchName());
            record.setUserId(dto.getCreatorUserId());
            record.setTargetEnv(envCode.getCode());
            record.setTargetBranch(envCode.getBranchName());
            record.setStatus(MergeStatus.PENDING.getCode());
            record.setCrRequired(envCode == EnvCode.PREVIEW); // 预发环境需CR审核才能进入正式环境
            record.setCreateTime(LocalDateTime.now());
            record.setUpdateTime(LocalDateTime.now());
            mergeRecordMapper.insert(record);
            if (envCode == EnvCode.DEV) {
                devRecord = record;
            }
        }

        operationLogService.record(project.getProjectId(), requirement.getRequirementId(),
                devRecord != null ? devRecord.getMergeId() : null,
                dto.getCreatorUserId(),
                source == BranchSource.NEW ? OperationAction.CREATE_BRANCH : OperationAction.PULL_BRANCH,
                (source == BranchSource.NEW ? "新建" : "拉取") + "分支 " + requirement.getBranchName());
        operationLogService.record(project.getProjectId(), requirement.getRequirementId(),
                devRecord != null ? devRecord.getMergeId() : null,
                dto.getCreatorUserId(),
                OperationAction.CREATE_REQUIREMENT,
                "创建需求：" + requirement.getRequirementName() + "（分支 " + requirement.getBranchName()
                        + "，已进入 开发/测试/预发 三个环境待集成列表）");
        return requirement;
    }

    @Override
    public List<CvmRequirement> listByProject(Long projectId) {
        return requirementMapper.selectByProjectId(projectId);
    }

    @Override
    public List<CvmRequirement> listByUser(Long creatorUserId) {
        return requirementMapper.selectByCreatorUserId(creatorUserId);
    }

    @Override
    public CvmRequirement getById(Long requirementId) {
        CvmRequirement requirement = requirementMapper.selectByRequirementId(requirementId);
        if (requirement == null) {
            throw new RuntimeException("需求不存在");
        }
        return requirement;
    }
}
