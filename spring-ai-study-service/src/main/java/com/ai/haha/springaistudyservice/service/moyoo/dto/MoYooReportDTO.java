package com.ai.haha.springaistudyservice.service.moyoo.dto;

import lombok.Data;

/**
 * 摸鱼报告DTO
 */
@Data
public class MoYooReportDTO {
    private Long recordId;
    private Double durationMinutes; // 支持小数
    private Double carbonReductionGrams;
    private Double lifeExtensionMinutes;
    private String aiReport;
}

