package com.ai.haha.springaistudyservice.service.moyoo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 摸鱼成果实体类
 */
@Data
@TableName("moyoo_result")
public class MoYooResult {
    
    /**
     * 主键ID（自增）
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 成果ID（雪花算法生成，业务ID）
     * 使用字符串格式返回，避免JavaScript大整数精度丢失
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long resultId;
    
    /**
     * 记录ID（关联摸鱼记录）
     * 使用字符串格式返回，避免JavaScript大整数精度丢失
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long recordId;
    
    /**
     * 用户ID
     * 使用字符串格式返回，避免JavaScript大整数精度丢失
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long userId;
    
    /**
     * 摸鱼时长（分钟，支持小数）
     */
    private Double durationMinutes;
    
    /**
     * 减少碳排放（克）
     */
    private Double carbonReductionGrams;
    
    /**
     * 寿命增加（分钟）
     */
    private Double lifeExtensionMinutes;
    
    /**
     * AI生成的报告内容
     */
    private String aiReport;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}

