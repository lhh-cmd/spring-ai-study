package com.ai.haha.springaistudyservice.service.cvm.service;

import com.ai.haha.springaistudyservice.service.cvm.dto.CvmCrAuditDTO;
import com.ai.haha.springaistudyservice.service.cvm.dto.CvmCrItemDTO;
import com.ai.haha.springaistudyservice.service.cvm.dto.CvmCrSubmitDTO;
import com.ai.haha.springaistudyservice.service.cvm.entity.CvmCrRecord;
import com.ai.haha.springaistudyservice.service.cvm.entity.CvmUser;

import java.util.List;

/**
 * CVM代码评审(CR)服务接口
 */
public interface CvmCrService {

    /**
     * 发起CR：将分支提交给指定评审人审核（生成 PENDING 记录）
     */
    void submit(CvmCrSubmitDTO dto);

    /**
     * CR 审核（PASS/REJECT），仅被指定的评审人可操作
     */
    void audit(CvmCrAuditDTO dto);

    /**
     * 判断指定合并记录是否已通过CR
     */
    boolean hasPassed(Long mergeId);

    /**
     * 查询合并记录的所有CR记录
     */
    List<CvmCrRecord> listByMerge(Long mergeId);

    /**
     * 我发起的CR列表
     */
    List<CvmCrItemDTO> listSubmitted(Long userId);

    /**
     * 需要我审核的CR列表（待审核）
     */
    List<CvmCrItemDTO> listPendingByReviewer(Long userId);

    /**
     * 项目可指定的CR评审人列表（当前为系统全部用户）
     */
    List<CvmUser> listReviewers(Long projectId);
}
