package com.ai.haha.springaistudyservice.service.cvm.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * CVM操作日志展示视图（补充项目名、操作人昵称）
 */
@Data
public class CvmOperationLogView {

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
     * 项目名称
     */
    private String projectName;

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
     * 操作人昵称
     */
    private String operatorName;

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