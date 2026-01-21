package com.ai.haha.springaistudyservice.service.moyoo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

/**
 * 榜单DTO
 */
@Data
public class RankingDTO {
    /**
     * 用户ID
     * 使用字符串格式返回，避免JavaScript大整数精度丢失
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long userId;
    private String username;
    private String profession;
    private Double value; // 摸鱼时长（分钟，支持小数）或次数
    private Double carbonReduction; // 减少碳排放（克）
}

