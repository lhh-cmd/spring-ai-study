package com.ai.haha.springaistudyservice.service.moyoo.dto;

import lombok.Data;

/**
 * 用户注册DTO
 */
@Data
public class UserRegisterDTO {
    private String username;
    private String password;
    private String profession;
    private Double dailyWorkHours;
}

