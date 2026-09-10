package com.ai.haha.springaistudyservice.service.cvm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * CVM代码评审(CR)记录实体类
 */
@Data
@TableName("cvm_cr_record")
public class CvmCrRecord {

    /**
     * 主键ID（自增）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * CR记录ID（雪花算法生成，业务ID）
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long crId;

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
     * 关联合并记录ID
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long mergeId;

    /**
     * CR发起人用户ID
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long submitterUserId;

    /**
     * 被指定的评审人用户ID
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long reviewerUserId;

    /**
     * CR状态（PENDING待审核/PASS通过/REJECT驳回）
     */
    private String crStatus;

    /**
     * 审核意见
     */
    private String crComment;

    /**
     * 审核时间
     */
    private LocalDateTime crTime;

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
