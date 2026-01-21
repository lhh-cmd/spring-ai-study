package com.ai.haha.springaistudyservice.service.moyoo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体类
 */
@Data
@TableName("moyoo_user")
public class User {
    
    /**
     * 主键ID（自增）
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 用户ID（雪花算法生成，业务ID）
     * 使用字符串格式返回，避免JavaScript大整数精度丢失
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long userId;
    
    /**
     * 用户名（唯一）
     */
    private String username;
    
    /**
     * 密码
     */
    private String password;
    
    /**
     * 职业
     */
    private String profession;
    
    /**
     * 日工作时长（小时）
     */
    private Double dailyWorkHours;
    
    /**
     * 用户状态（0-离线，1-在线，2-摸鱼中）
     */
    private Integer status;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}

