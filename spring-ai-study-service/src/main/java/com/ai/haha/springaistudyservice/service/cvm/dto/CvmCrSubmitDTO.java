package com.ai.haha.springaistudyservice.service.cvm.dto;

import lombok.Data;

/**
 * CVM代码评审(CR)发起请求DTO
 */
@Data
public class CvmCrSubmitDTO {

    /**
     * 合并记录ID（业务ID）
     */
    private Long mergeId;

    /**
     * CR发起人用户ID（业务ID）
     */
    private Long submitterUserId;

    /**
     * 被指定的评审人用户ID（业务ID）
     */
    private Long reviewerUserId;

    /**
     * CR说明/请求备注
     */
    private String crComment;
}
