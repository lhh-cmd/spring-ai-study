package com.ai.haha.springaistudyservice.service.cvm.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * CVM合并记录视图DTO（含操作人姓名、CR状态，供前端展示）
 */
@Data
public class CvmMergeRecordView {

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long mergeId;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long projectId;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long requirementId;

    private String requirementName;

    private String branchName;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long userId;

    /**
     * 操作人姓名（昵称或用户名）
     */
    private String userName;

    private String targetEnv;

    private String targetBranch;

    private String status;

    private String statusDesc;

    private List<String> conflictFiles;

    private String conflictDetail;

    private String resolveSteps;

    private String mergeCommit;

    private Boolean crRequired;

    /**
     * 当前合并记录的CR状态（NONE/PENDING/PASS/REJECT）
     */
    private String crStatus;

    private LocalDateTime mergeTime;

    private LocalDateTime createTime;
}
