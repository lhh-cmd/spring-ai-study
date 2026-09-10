package com.ai.haha.springaistudyservice.service.cvm.dto;

import lombok.Data;

/**
 * CVM创建需求请求DTO
 */
@Data
public class CvmRequirementCreateDTO {

    /**
     * 项目ID（业务ID）
     */
    private Long projectId;

    /**
     * 需求名称
     */
    private String requirementName;

    /**
     * 需求地址
     */
    private String requirementUrl;

    /**
     * 开发分支名
     */
    private String branchName;

    /**
     * 分支来源（NEW新建/REMOTE远程已有拉取）
     */
    private String branchSource;

    /**
     * 创建人用户ID（业务ID）
     */
    private Long creatorUserId;
}
