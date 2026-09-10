package com.ai.haha.springaistudyservice.service.cvm.service.impl;

import com.ai.haha.springaistudyservice.service.cvm.dto.CvmCrAuditDTO;
import com.ai.haha.springaistudyservice.service.cvm.dto.CvmCrItemDTO;
import com.ai.haha.springaistudyservice.service.cvm.dto.CvmCrSubmitDTO;
import com.ai.haha.springaistudyservice.service.cvm.entity.CvmCrRecord;
import com.ai.haha.springaistudyservice.service.cvm.entity.CvmMergeRecord;
import com.ai.haha.springaistudyservice.service.cvm.entity.CvmUser;
import com.ai.haha.springaistudyservice.service.cvm.enums.CrStatus;
import com.ai.haha.springaistudyservice.service.cvm.enums.MergeStatus;
import com.ai.haha.springaistudyservice.service.cvm.enums.OperationAction;
import com.ai.haha.springaistudyservice.service.cvm.mapper.CvmCrRecordMapper;
import com.ai.haha.springaistudyservice.service.cvm.mapper.CvmMergeRecordMapper;
import com.ai.haha.springaistudyservice.service.cvm.mapper.CvmUserMapper;
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
    private CvmUserMapper cvmUserMapper;

    @Resource
    private CvmOperationLogService operationLogService;

    @Resource
    private SnowflakeIdGenerator idGenerator;

    @Override
    @Transactional
    public void submit(CvmCrSubmitDTO dto) {
        if (dto.getMergeId() == null) {
            throw new RuntimeException("合并记录不能为空");
        }
        CvmMergeRecord record = mergeRecordMapper.selectByMergeId(dto.getMergeId());
        if (record == null) {
            throw new RuntimeException("合并记录不存在");
        }
        String recordStatus = record.getStatus();
        if (!MergeStatus.PENDING.getCode().equals(recordStatus)
                && !MergeStatus.MERGED.getCode().equals(recordStatus)
                && !MergeStatus.REJECTED.getCode().equals(recordStatus)) {
            throw new RuntimeException("仅待集成或已集成的分支可以发起CR");
        }
        if (dto.getSubmitterUserId() == null) {
            throw new RuntimeException("CR发起人不能为空");
        }
        if (dto.getReviewerUserId() == null) {
            throw new RuntimeException("请选择CR审核人");
        }
        if (dto.getReviewerUserId().equals(dto.getSubmitterUserId())) {
            throw new RuntimeException("不能指定自己审核自己的代码");
        }
        if (crRecordMapper.hasPassedByRequirement(record.getRequirementId())) {
            throw new RuntimeException("该分支CR已通过，无需重复发起");
        }
        if (crRecordMapper.hasPendingByRequirement(record.getRequirementId())) {
            throw new RuntimeException("该分支已有待审核的CR，请等待审核完成");
        }

        CvmCrRecord cr = new CvmCrRecord();
        cr.setCrId(idGenerator.nextId());
        cr.setProjectId(record.getProjectId());
        cr.setRequirementId(record.getRequirementId());
        cr.setMergeId(record.getMergeId());
        cr.setSubmitterUserId(dto.getSubmitterUserId());
        cr.setReviewerUserId(dto.getReviewerUserId());
        cr.setCrStatus(CrStatus.PENDING.getCode());
        cr.setCrComment(dto.getCrComment());
        cr.setCreateTime(LocalDateTime.now());
        cr.setUpdateTime(LocalDateTime.now());
        crRecordMapper.insert(cr);

        CvmUser reviewer = cvmUserMapper.selectByUserId(dto.getReviewerUserId());
        String reviewerName = reviewer != null && StringUtils.hasText(reviewer.getNickname())
                ? reviewer.getNickname() : reviewer != null ? reviewer.getUsername() : String.valueOf(dto.getReviewerUserId());
        operationLogService.record(record.getProjectId(), record.getRequirementId(), record.getMergeId(),
                dto.getSubmitterUserId(), OperationAction.CR_SUBMIT,
                "发起CR：" + record.getBranchName() + " → 审核人：" + reviewerName
                        + (StringUtils.hasText(dto.getCrComment()) ? "，说明：" + dto.getCrComment() : ""));
    }

    @Override
    @Transactional
    public void audit(CvmCrAuditDTO dto) {
        if (dto.getMergeId() == null || dto.getReviewerUserId() == null) {
            throw new RuntimeException("审核信息不完整");
        }
        CvmCrRecord cr = crRecordMapper.selectPendingByMergeAndReviewer(dto.getMergeId(), dto.getReviewerUserId());
        if (cr == null) {
            throw new RuntimeException("未找到该分支分配给您的待审核CR");
        }
        CrStatus status;
        try {
            status = CrStatus.valueOf(dto.getCrStatus());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("审核结果无效：" + dto.getCrStatus());
        }
        if (status == CrStatus.PENDING) {
            throw new RuntimeException("审核结果无效：" + dto.getCrStatus());
        }
        cr.setCrStatus(status.getCode());
        cr.setCrComment(dto.getCrComment());
        cr.setCrTime(LocalDateTime.now());
        cr.setUpdateTime(LocalDateTime.now());
        crRecordMapper.updateById(cr);

        // 仅对待合并记录（非已合并）调整状态；预发环境CR针对已合并记录进行，不改变其合并状态
        CvmMergeRecord record = mergeRecordMapper.selectByMergeId(dto.getMergeId());
        if (record != null) {
            boolean isMerged = MergeStatus.MERGED.getCode().equals(record.getStatus());
            if (status == CrStatus.REJECT) {
                if (!isMerged) {
                    record.setStatus(MergeStatus.REJECTED.getCode());
                }
            } else if (MergeStatus.REJECTED.getCode().equals(record.getStatus())) {
                record.setStatus(MergeStatus.PENDING.getCode());
            }
            record.setUpdateTime(LocalDateTime.now());
            mergeRecordMapper.updateById(record);
        }

        operationLogService.record(cr.getProjectId(), cr.getRequirementId(), cr.getMergeId(),
                dto.getReviewerUserId(),
                status == CrStatus.PASS ? OperationAction.CR_PASS : OperationAction.CR_REJECT,
                (status == CrStatus.PASS ? "CR通过" : "CR驳回") + "：" + (record != null ? record.getBranchName() : "")
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

    @Override
    public List<CvmCrItemDTO> listSubmitted(Long userId) {
        return crRecordMapper.selectSubmittedByUserId(userId);
    }

    @Override
    public List<CvmCrItemDTO> listPendingByReviewer(Long userId) {
        return crRecordMapper.selectPendingByReviewerUserId(userId);
    }

    @Override
    public List<CvmUser> listReviewers(Long projectId) {
        return cvmUserMapper.selectList(null);
    }
}
