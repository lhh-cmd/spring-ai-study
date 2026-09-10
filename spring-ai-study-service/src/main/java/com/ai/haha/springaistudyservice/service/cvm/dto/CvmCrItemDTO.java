package com.ai.haha.springaistudyservice.service.cvm.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * CVM代码评审(CR)列表项DTO（CR管理页展示，关联分支/需求/用户信息）
 */
@Data
public class CvmCrItemDTO {

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long crId;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long mergeId;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long projectId;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long requirementId;

    private String branchName;

    private String requirementName;

    private String targetEnv;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long submitterUserId;

    private String submitterName;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long reviewerUserId;

    private String reviewerName;

    private String crStatus;

    private String crComment;

    private LocalDateTime createTime;

    private LocalDateTime crTime;
}
