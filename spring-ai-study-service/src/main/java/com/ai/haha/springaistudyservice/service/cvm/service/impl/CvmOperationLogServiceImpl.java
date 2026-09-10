package com.ai.haha.springaistudyservice.service.cvm.service.impl;

import com.ai.haha.springaistudyservice.service.cvm.entity.CvmOperationLog;
import com.ai.haha.springaistudyservice.service.cvm.enums.OperationAction;
import com.ai.haha.springaistudyservice.service.cvm.mapper.CvmOperationLogMapper;
import com.ai.haha.springaistudyservice.service.cvm.service.CvmOperationLogService;
import com.ai.haha.springaistudyservice.service.moyoo.util.SnowflakeIdGenerator;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * CVM操作日志服务实现
 */
@Service
public class CvmOperationLogServiceImpl implements CvmOperationLogService {

    @Resource
    private CvmOperationLogMapper operationLogMapper;

    @Resource
    private SnowflakeIdGenerator idGenerator;

    @Override
    public void record(Long projectId, Long requirementId, Long mergeId, Long operatorUserId, OperationAction action, String detail) {
        CvmOperationLog log = new CvmOperationLog();
        log.setLogId(idGenerator.nextId());
        log.setProjectId(projectId);
        log.setRequirementId(requirementId);
        log.setMergeId(mergeId);
        log.setOperatorUserId(operatorUserId);
        log.setAction(action.name());
        log.setDetail(detail);
        log.setCreateTime(LocalDateTime.now());
        operationLogMapper.insert(log);
    }

    @Override
    public List<CvmOperationLog> listByProject(Long projectId) {
        return operationLogMapper.selectByProjectId(projectId);
    }

    @Override
    public List<CvmOperationLog> listAll() {
        return operationLogMapper.selectAllOrderByCreateTime();
    }
}
