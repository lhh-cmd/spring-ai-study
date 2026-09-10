package com.ai.haha.springaistudyservice.service.cvm.service;

import com.ai.haha.springaistudyservice.service.cvm.dto.CvmOperationLogView;
import com.ai.haha.springaistudyservice.service.cvm.enums.OperationAction;

import java.util.List;

/**
 * CVM操作日志服务接口
 */
public interface CvmOperationLogService {

    /**
     * 记录操作日志
     */
    void record(Long projectId, Long requirementId, Long mergeId, Long operatorUserId, OperationAction action, String detail);

    /**
     * 根据项目查询日志（按时间倒序，含项目名/操作人）
     */
    List<CvmOperationLogView> listByProject(Long projectId);

    /**
     * 查询全部日志（按时间倒序，含项目名/操作人）
     */
    List<CvmOperationLogView> listAll();
}