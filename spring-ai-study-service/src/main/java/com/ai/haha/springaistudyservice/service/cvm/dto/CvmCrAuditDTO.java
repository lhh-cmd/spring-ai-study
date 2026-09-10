package com.ai.haha.springaistudyservice.service.cvm.dto;

import lombok.Data;

/**
 * CVM代码评审(CR)审核请求DTO
 */
@Data
public class CvmCrAuditDTO {

    /**
     * 合并记录ID（业务ID）
     */
    private Long mergeId;

    /**
     * 评审人用户ID（业务ID）
     */
    private Long reviewerUserId;

    /**
     * 审核结果（PASS/REJECT）
     */
    private String crStatus;

    /**
     * 审核意见
     */
    private String crComment;
}
