package com.ai.haha.springaistudyservice.service.cvm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * CVM分支登记实体类
 */
@Data
@TableName("cvm_branch")
public class CvmBranch {

    /**
     * 主键ID（自增）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 分支ID（雪花算法生成，业务ID）
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long branchId;

    /**
     * 项目ID
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long projectId;

    /**
     * 分支名
     */
    private String branchName;

    /**
     * 分支类型（ENV_BRANCH/REQUIREMENT_BRANCH/MASTER）
     */
    private String branchType;

    /**
     * 基于的分支
     */
    private String baseBranch;

    /**
     * 来源（CREATED新建/PULLED远程拉取）
     */
    private String source;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
