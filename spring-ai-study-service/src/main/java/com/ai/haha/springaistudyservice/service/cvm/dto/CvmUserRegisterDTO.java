package com.ai.haha.springaistudyservice.service.cvm.dto;

import lombok.Data;

/**
 * CVM用户注册请求DTO
 */
@Data
public class CvmUserRegisterDTO {

    private String username;

    private String password;

    private String nickname;
}
