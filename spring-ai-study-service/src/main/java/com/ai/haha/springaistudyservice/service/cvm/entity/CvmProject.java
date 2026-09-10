package com.ai.haha.springaistudyservice.service.cvm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * CVM项目实体类
 */
@Data
@TableName("cvm_project")
public class CvmProject {

    /**
     * 主键ID（自增）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 项目ID（雪花算法生成，业务ID）
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long projectId;

    /**
     * 项目编号（用户填写的项目ID）
     */
    private String projectCode;

    /**
     * 项目名称
     */
    private String projectName;

    /**
     * 项目描述
     */
    private String projectDesc;

    /**
     * 公司项目Git地址（可空）
     */
    private String gitUrl;

    /**
     * Git账号（可空）
     */
    private String gitUsername;

    /**
     * Git令牌/密码（可空）
     */
    private String gitToken;

    /**
     * 创建人用户ID
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long creatorUserId;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
