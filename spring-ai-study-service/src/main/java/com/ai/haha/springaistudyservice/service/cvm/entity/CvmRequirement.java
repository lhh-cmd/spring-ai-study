package com.ai.haha.springaistudyservice.service.cvm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * CVM需求实体类
 */
@Data
@TableName("cvm_requirement")
public class CvmRequirement {

    /**
     * 主键ID（自增）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 需求ID（雪花算法生成，业务ID）
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long requirementId;

    /**
     * 项目ID
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long projectId;

    /**
     * 需求名称
     */
    private String requirementName;

    /**
     * 需求地址
     */
    private String requirementUrl;

    /**
     * 开发分支名
     */
    private String branchName;

    /**
     * 分支来源（NEW新建/REMOTE远程拉取）
     */
    private String branchSource;

    /**
     * 创建人用户ID
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long creatorUserId;

    /**
     * 当前所在环境（DEV/TEST/PREVIEW/RELEASE）
     */
    private String currentEnv;

    /**
     * 需求状态（DEVELOPING/CONFLICT/MERGED/RELEASED）
     */
    private String status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 逻辑删除标记（0正常/1已删除）
     */
    @TableLogic
    private Integer deleted;
}
