package com.ai.haha.springaistudyservice.service.cvm.dto;

import lombok.Data;

/**
 * CVM项目注册请求DTO
 */
@Data
public class CvmProjectCreateDTO {

    /**
     * 项目编号（用户填写的项目ID）
     */
    private String projectCode;

    /**
     * 项目名称
     */
    private String projectName;

    /**
     * 项目描述
     */
    private String projectDesc;

    /**
     * 公司项目Git地址（可空）
     */
    private String gitUrl;

    /**
     * Git账号（可空）
     */
    private String gitUsername;

    /**
     * Git令牌/密码（可空）
     */
    private String gitToken;
}
