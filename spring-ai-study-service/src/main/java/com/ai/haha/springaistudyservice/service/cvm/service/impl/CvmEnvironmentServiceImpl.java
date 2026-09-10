package com.ai.haha.springaistudyservice.service.cvm.service.impl;

import com.ai.haha.springaistudyservice.service.cvm.entity.CvmEnvironment;
import com.ai.haha.springaistudyservice.service.cvm.entity.CvmProject;
import com.ai.haha.springaistudyservice.service.cvm.enums.EnvCode;
import com.ai.haha.springaistudyservice.service.cvm.enums.OperationAction;
import com.ai.haha.springaistudyservice.service.cvm.git.GitOperationService;
import com.ai.haha.springaistudyservice.service.cvm.git.GitServiceFactory;
import com.ai.haha.springaistudyservice.service.cvm.mapper.CvmEnvironmentMapper;
import com.ai.haha.springaistudyservice.service.cvm.service.CvmEnvironmentService;
import com.ai.haha.springaistudyservice.service.cvm.service.CvmOperationLogService;
import com.ai.haha.springaistudyservice.service.moyoo.util.SnowflakeIdGenerator;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * CVM环境服务实现
 */
@Service
public class CvmEnvironmentServiceImpl implements CvmEnvironmentService {

    private static final Logger log = LoggerFactory.getLogger(CvmEnvironmentServiceImpl.class);

    @Resource
    private CvmEnvironmentMapper environmentMapper;

    @Resource
    private GitServiceFactory gitServiceFactory;

    @Resource
    private SnowflakeIdGenerator idGenerator;

    @Resource
    private CvmOperationLogService operationLogService;

    @Override
    @Transactional
    public void initStandardEnvironments(CvmProject project) {
        GitOperationService git = gitServiceFactory.getService(project);
        // 确保 master 分支存在
        ensureBranch(project, git, "master", "master");
        for (EnvCode envCode : EnvCode.values()) {
            CvmEnvironment env = new CvmEnvironment();
            env.setEnvId(idGenerator.nextId());
            env.setProjectId(project.getProjectId());
            env.setEnvCode(envCode.getCode());
            env.setEnvName(envCode.getDescription());
            env.setBranchName(envCode.getBranchName());
            env.setSortOrder(envCode.getSortOrder());
            env.setCreateTime(LocalDateTime.now());
            env.setUpdateTime(LocalDateTime.now());
            environmentMapper.insert(env);
            ensureBranch(project, git, envCode.getBranchName(), "master");
            operationLogService.record(project.getProjectId(), null, null, project.getCreatorUserId(),
                    OperationAction.INIT_ENVIRONMENT, "初始化环境：" + envCode.getDescription() + "（分支 " + envCode.getBranchName() + "）");
        }
    }

    @Override
    public List<CvmEnvironment> listByProject(Long projectId) {
        return environmentMapper.selectByProjectId(projectId);
    }

    @Override
    public CvmEnvironment getByProjectAndCode(Long projectId, String envCode) {
        return environmentMapper.selectByProjectIdAndEnvCode(projectId, envCode);
    }

    /**
     * 确保分支存在：远程存在则不再创建，否则创建（创建失败不影响环境记录，避免阻塞项目注册）
     */
    private void ensureBranch(CvmProject project, GitOperationService git, String branch, String baseBranch) {
        try {
            if (!git.checkBranchExists(project, branch)) {
                git.createBranch(project, branch, baseBranch);
            }
        } catch (Exception e) {
            log.warn("初始化分支失败（不影响环境记录）：项目={}, 分支={}, 原因={}", project.getProjectCode(), branch, e.getMessage());
        }
    }
}
