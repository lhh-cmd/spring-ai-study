package com.ai.haha.springaistudyservice.service.cvm.dto;

import lombok.Data;

/**
 * CVM用户登录请求DTO
 */
@Data
public class CvmUserLoginDTO {

    private String username;

    private String password;
}
