package com.ai.haha.springaistudyservice.service.cvm.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * CVM合并记录实体类（待合并列表/已合并历史）
 */
@Data
@TableName("cvm_merge_record")
public class CvmMergeRecord {

    /**
     * 主键ID（自增）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 合并记录ID（雪花算法生成，业务ID）
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long mergeId;

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
     * 需求名称
     */
    private String requirementName;

    /**
     * 要合并的开发分支
     */
    private String branchName;

    /**
     * 操作人用户ID
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long userId;

    /**
     * 目标环境（DEV/TEST/PREVIEW/RELEASE）
     */
    private String targetEnv;

    /**
     * 目标公共分支（dev/test/preview/release）
     */
    private String targetBranch;

    /**
     * 状态（PENDING/CONFLICT/MERGED/REJECTED）
     */
    private String status;

    /**
     * 冲突文件列表（JSON数组）
     */
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private String conflictFiles;

    /**
     * 冲突详情
     */
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private String conflictDetail;

    /**
     * 解决冲突步骤（给用户展示）
     */
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private String resolveSteps;

    /**
     * 合并后的提交ID
     */
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private String mergeCommit;

    /**
     * 是否需要CR审核（release环境=1）
     */
    private Boolean crRequired;

    /**
     * 合并时间
     */
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private LocalDateTime mergeTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
