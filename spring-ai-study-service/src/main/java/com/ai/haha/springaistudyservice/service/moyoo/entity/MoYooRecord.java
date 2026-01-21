package com.ai.haha.springaistudyservice.service.moyoo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 摸鱼记录实体类
 */
@Data
@TableName("moyoo_record")
public class MoYooRecord {
    
    /**
     * 主键ID（自增）
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 记录ID（雪花算法生成，业务ID）
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
     * 开始时间
     */
    private LocalDateTime startTime;
    
    /**
     * 结束时间
     */
    private LocalDateTime endTime;
    
    /**
     * 摸鱼时长（分钟，支持小数）
     */
    private Double durationMinutes;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}

