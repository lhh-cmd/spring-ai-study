package com.ai.haha.springaistudyservice.service.cvm.service.impl;

import com.ai.haha.springaistudyservice.service.cvm.dto.CvmCrAuditDTO;
import com.ai.haha.springaistudyservice.service.cvm.entity.CvmCrRecord;
import com.ai.haha.springaistudyservice.service.cvm.entity.CvmMergeRecord;
import com.ai.haha.springaistudyservice.service.cvm.enums.CrStatus;
import com.ai.haha.springaistudyservice.service.cvm.enums.MergeStatus;
import com.ai.haha.springaistudyservice.service.cvm.enums.OperationAction;
import com.ai.haha.springaistudyservice.service.cvm.mapper.CvmCrRecordMapper;
import com.ai.haha.springaistudyservice.service.cvm.mapper.CvmMergeRecordMapper;
import com.ai.haha.springaistudyservice.service.cvm.service.CvmCrService;
import com.ai.haha.springaistudyservice.service.cvm.service.CvmOperationLogService;
import com.ai.haha.springaistudyservice.service.moyoo.util.SnowflakeIdGenerator;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * CVM代码评审(CR)服务实现
 */
@Service
public class CvmCrServiceImpl implements CvmCrService {

    @Resource
    private CvmCrRecordMapper crRecordMapper;

    @Resource
    private CvmMergeRecordMapper mergeRecordMapper;

    @Resource
    private CvmOperationLogService operationLogService;

    @Resource
    private SnowflakeIdGenerator idGenerator;

    @Override
    @Transactional
    public void audit(CvmCrAuditDTO dto) {
        if (dto.getMergeId() == null) {
            throw new RuntimeException("合并记录不能为空");
        }
        CvmMergeRecord record = mergeRecordMapper.selectByMergeId(dto.getMergeId());
        if (record == null) {
            throw new RuntimeException("合并记录不存在");
        }
        CrStatus status;
        try {
            status = CrStatus.valueOf(dto.getCrStatus());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("审核结果无效：" + dto.getCrStatus());
        }

        CvmCrRecord cr = new CvmCrRecord();
        cr.setCrId(idGenerator.nextId());
        cr.setProjectId(record.getProjectId());
        cr.setRequirementId(record.getRequirementId());
        cr.setMergeId(record.getMergeId());
        cr.setReviewerUserId(dto.getReviewerUserId());
        cr.setCrStatus(status.getCode());
        cr.setCrComment(dto.getCrComment());
        cr.setCrTime(LocalDateTime.now());
        cr.setCreateTime(LocalDateTime.now());
        cr.setUpdateTime(LocalDateTime.now());
        crRecordMapper.insert(cr);

        // 驳回则待合并记录置为已驳回；通过时若之前被驳回则恢复为待合并
        if (status == CrStatus.REJECT) {
            record.setStatus(MergeStatus.REJECTED.getCode());
        } else if (MergeStatus.REJECTED.getCode().equals(record.getStatus())) {
            record.setStatus(MergeStatus.PENDING.getCode());
        }
        record.setUpdateTime(LocalDateTime.now());
        mergeRecordMapper.updateById(record);

        operationLogService.record(record.getProjectId(), record.getRequirementId(), record.getMergeId(),
                dto.getReviewerUserId(),
                status == CrStatus.PASS ? OperationAction.CR_PASS : OperationAction.CR_REJECT,
                (status == CrStatus.PASS ? "CR通过" : "CR驳回") + "：" + record.getBranchName()
                        + (StringUtils.hasText(dto.getCrComment()) ? "，意见：" + dto.getCrComment() : ""));
    }

    @Override
    public boolean hasPassed(Long mergeId) {
        return crRecordMapper.hasPassed(mergeId);
    }

    @Override
    public List<CvmCrRecord> listByMerge(Long mergeId) {
        return crRecordMapper.selectByMergeId(mergeId);
    }
}
