package com.ai.haha.springaistudyservice.service.cvm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * CVM环境实体类
 */
@Data
@TableName("cvm_environment")
public class CvmEnvironment {

    /**
     * 主键ID（自增）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 环境ID（雪花算法生成，业务ID）
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long envId;

    /**
     * 项目ID
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long projectId;

    /**
     * 环境编码（DEV/TEST/PREVIEW/RELEASE）
     */
    private String envCode;

    /**
     * 环境名称（开发/测试/预发/正式）
     */
    private String envName;

    /**
     * 公共分支名（dev/test/preview/release）
     */
    private String branchName;

    /**
     * 排序（1-dev,2-test,3-preview,4-release）
     */
    private Integer sortOrder;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
