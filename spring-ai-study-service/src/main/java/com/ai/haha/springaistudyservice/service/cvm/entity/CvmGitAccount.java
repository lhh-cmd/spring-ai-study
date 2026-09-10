package com.ai.haha.springaistudyservice.service.cvm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * CVM全局Git服务账号实体类。
 *
 * <p>所有项目的建分支/推送/合并统一使用该账号（token 存 DB 不落配置文件），
 * 项目注册时用户只需提供 Git 地址。</p>
 */
@Data
@TableName("cvm_git_account")
public class CvmGitAccount {

    /**
     * 主键ID（自增）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 账号ID（雪花算法生成，业务ID）
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long accountId;

    /**
     * Git服务账号用户名（如 lhh-cmd / haixing）
     */
    private String accountName;

    /**
     * Git访问令牌（Personal Access Token）
     */
    private String gitToken;

    /**
     * 备注
     */
    private String remark;

    /**
     * 是否启用（1启用/0停用）
     */
    private Integer active;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
