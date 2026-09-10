package com.ai.haha.springaistudyservice.service.cvm.service.impl;

import com.ai.haha.springaistudyservice.service.cvm.dto.CvmOperationLogView;
import com.ai.haha.springaistudyservice.service.cvm.entity.CvmOperationLog;
import com.ai.haha.springaistudyservice.service.cvm.entity.CvmProject;
import com.ai.haha.springaistudyservice.service.cvm.entity.CvmUser;
import com.ai.haha.springaistudyservice.service.cvm.enums.OperationAction;
import com.ai.haha.springaistudyservice.service.cvm.mapper.CvmOperationLogMapper;
import com.ai.haha.springaistudyservice.service.cvm.mapper.CvmProjectMapper;
import com.ai.haha.springaistudyservice.service.cvm.mapper.CvmUserMapper;
import com.ai.haha.springaistudyservice.service.cvm.service.CvmOperationLogService;
import com.ai.haha.springaistudyservice.service.moyoo.util.SnowflakeIdGenerator;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CVM操作日志服务实现
 */
@Service
public class CvmOperationLogServiceImpl implements CvmOperationLogService {

    @Resource
    private CvmOperationLogMapper operationLogMapper;

    @Resource
    private CvmProjectMapper projectMapper;

    @Resource
    private CvmUserMapper cvmUserMapper;

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
    public List<CvmOperationLogView> listByProject(Long projectId) {
        return toViews(operationLogMapper.selectByProjectId(projectId));
    }

    @Override
    public List<CvmOperationLogView> listAll() {
        return toViews(operationLogMapper.selectAllOrderByCreateTime());
    }

    /**
     * 日志列表补充项目名与操作人昵称
     */
    private List<CvmOperationLogView> toViews(List<CvmOperationLog> logs) {
        Map<Long, String> projectNames = new HashMap<>();
        for (CvmProject p : projectMapper.selectList(null)) {
            projectNames.put(p.getProjectId(), p.getProjectName());
        }
        Map<Long, String> userNames = new HashMap<>();
        for (CvmUser u : cvmUserMapper.selectList(null)) {
            userNames.put(u.getUserId(), StringUtils.hasText(u.getNickname()) ? u.getNickname() : u.getUsername());
        }

        List<CvmOperationLogView> views = new ArrayList<>(logs.size());
        for (CvmOperationLog log : logs) {
            CvmOperationLogView view = new CvmOperationLogView();
            view.setLogId(log.getLogId());
            view.setProjectId(log.getProjectId());
            view.setProjectName(projectNames.get(log.getProjectId()));
            view.setRequirementId(log.getRequirementId());
            view.setMergeId(log.getMergeId());
            view.setOperatorUserId(log.getOperatorUserId());
            view.setOperatorName(userNames.get(log.getOperatorUserId()));
            view.setAction(log.getAction());
            view.setDetail(log.getDetail());
            view.setCreateTime(log.getCreateTime());
            views.add(view);
        }
        return views;
    }
}