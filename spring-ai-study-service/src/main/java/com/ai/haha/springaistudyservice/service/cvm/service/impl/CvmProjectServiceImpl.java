package com.ai.haha.springaistudyservice.service.cvm.service.impl;

import com.ai.haha.springaistudyservice.service.cvm.dto.CvmProjectCreateDTO;
import com.ai.haha.springaistudyservice.service.cvm.dto.CvmProjectDetailDTO;
import com.ai.haha.springaistudyservice.service.cvm.entity.CvmProject;
import com.ai.haha.springaistudyservice.service.cvm.enums.OperationAction;
import com.ai.haha.springaistudyservice.service.cvm.mapper.CvmProjectMapper;
import com.ai.haha.springaistudyservice.service.cvm.mapper.CvmRequirementMapper;
import com.ai.haha.springaistudyservice.service.cvm.service.CvmEnvironmentService;
import com.ai.haha.springaistudyservice.service.cvm.service.CvmOperationLogService;
import com.ai.haha.springaistudyservice.service.cvm.service.CvmProjectService;
import com.ai.haha.springaistudyservice.service.moyoo.util.SnowflakeIdGenerator;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * CVM项目服务实现
 */
@Service
public class CvmProjectServiceImpl implements CvmProjectService {

    @Resource
    private CvmProjectMapper projectMapper;

    @Resource
    private CvmRequirementMapper requirementMapper;

    @Resource
    private CvmEnvironmentService environmentService;

    @Resource
    private CvmOperationLogService operationLogService;

    @Resource
    private SnowflakeIdGenerator idGenerator;

    @Override
    @Transactional
    public CvmProject createProject(CvmProjectCreateDTO dto, Long creatorUserId) {
        if (!StringUtils.hasText(dto.getProjectCode())) {
            throw new RuntimeException("项目编号（项目ID）不能为空");
        }
        if (!StringUtils.hasText(dto.getProjectName())) {
            throw new RuntimeException("项目名称不能为空");
        }
        if (projectMapper.existsByProjectCode(dto.getProjectCode())) {
            throw new RuntimeException("项目编号已存在：" + dto.getProjectCode());
        }
        CvmProject project = new CvmProject();
        project.setProjectId(idGenerator.nextId());
        project.setProjectCode(dto.getProjectCode());
        project.setProjectName(dto.getProjectName());
        project.setProjectDesc(dto.getProjectDesc());
        project.setGitUrl(dto.getGitUrl());
        project.setGitUsername(dto.getGitUsername());
        project.setGitToken(dto.getGitToken());
        project.setCreatorUserId(creatorUserId);
        project.setCreateTime(LocalDateTime.now());
        project.setUpdateTime(LocalDateTime.now());
        projectMapper.insert(project);

        // 初始化 4 个标准环境（dev/test/preview/release）
        environmentService.initStandardEnvironments(project);
        operationLogService.record(project.getProjectId(), null, null, creatorUserId,
                OperationAction.CREATE_PROJECT, "注册项目：" + project.getProjectName() + "（编号 " + project.getProjectCode() + "）");
        return project;
    }

    @Override
    public List<CvmProject> listProjects() {
        return projectMapper.selectAllOrderByCreateTime();
    }

    @Override
    public CvmProject getById(Long projectId) {
        CvmProject project = projectMapper.selectByProjectId(projectId);
        if (project == null) {
            throw new RuntimeException("项目不存在");
        }
        return project;
    }

    @Override
    public CvmProjectDetailDTO getProjectDetail(Long projectId) {
        CvmProject project = getById(projectId);
        CvmProjectDetailDTO dto = new CvmProjectDetailDTO();
        dto.setProject(project);
        dto.setEnvironments(environmentService.listByProject(projectId));
        dto.setRequirements(requirementMapper.selectByProjectId(projectId));
        return dto;
    }
}
