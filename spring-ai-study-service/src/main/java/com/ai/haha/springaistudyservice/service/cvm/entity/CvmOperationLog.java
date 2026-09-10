package com.ai.haha.springaistudyservice.service.cvm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * CVM操作日志实体类
 */
@Data
@TableName("cvm_operation_log")
public class CvmOperationLog {

    /**
     * 主键ID（自增）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 日志ID（雪花算法生成，业务ID）
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long logId;

    /**
     * 项目ID
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long projectId;

    /**
     * 需求ID
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long requirementId;

    /**
     * 合并记录ID
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long mergeId;

    /**
     * 操作人用户ID
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long operatorUserId;

    /**
     * 操作类型
     */
    private String action;

    /**
     * 操作详情
     */
    private String detail;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
