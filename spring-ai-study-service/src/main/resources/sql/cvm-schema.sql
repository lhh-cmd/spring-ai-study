-- 代码版本管理系统(CVM)数据库表结构

-- 全局Git服务账号表（所有项目的建分支/推送/合并统一使用该账号，账号和令牌存DB不落配置文件）
CREATE TABLE `cvm_git_account` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID（自增）',
    `account_id` BIGINT NOT NULL COMMENT '账号ID（雪花算法生成，业务ID）',
    `account_name` VARCHAR(100) NOT NULL COMMENT 'Git服务账号用户名（如 lhh-cmd / haixing）',
    `git_token` VARCHAR(255) NOT NULL COMMENT 'Git访问令牌（Personal Access Token）',
    `remark` VARCHAR(255) COMMENT '备注',
    `active` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用（1启用/0停用）',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_account_id` (`account_id`),
    UNIQUE KEY `uk_account_name` (`account_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CVM全局Git服务账号表';

-- 用户表
CREATE TABLE `cvm_user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID（自增）',
    `user_id` BIGINT NOT NULL COMMENT '用户ID（雪花算法生成，业务ID）',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名（唯一）',
    `password` VARCHAR(255) NOT NULL COMMENT '密码',
    `nickname` VARCHAR(50) COMMENT '昵称',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CVM用户表';

-- 项目表
CREATE TABLE `cvm_project` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID（自增）',
    `project_id` BIGINT NOT NULL COMMENT '项目ID（雪花算法生成，业务ID）',
    `project_code` VARCHAR(50) NOT NULL COMMENT '项目编号（用户填写的项目ID）',
    `project_name` VARCHAR(100) NOT NULL COMMENT '项目名称',
    `project_desc` VARCHAR(500) COMMENT '项目描述',
    `git_url` VARCHAR(500) COMMENT '公司项目Git地址（可空）',
    `git_username` VARCHAR(100) COMMENT 'Git账号（可空）',
    `git_token` VARCHAR(255) COMMENT 'Git令牌/密码（可空）',
    `creator_user_id` BIGINT NOT NULL COMMENT '创建人用户ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_project_id` (`project_id`),
    UNIQUE KEY `uk_project_code` (`project_code`),
    KEY `idx_creator` (`creator_user_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CVM项目表';

-- 环境表
CREATE TABLE `cvm_environment` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID（自增）',
    `env_id` BIGINT NOT NULL COMMENT '环境ID（雪花算法生成，业务ID）',
    `project_id` BIGINT NOT NULL COMMENT '项目ID',
    `env_code` VARCHAR(20) NOT NULL COMMENT '环境编码（DEV/TEST/PREVIEW/RELEASE）',
    `env_name` VARCHAR(50) NOT NULL COMMENT '环境名称（开发/测试/预发/正式）',
    `branch_name` VARCHAR(100) NOT NULL COMMENT '公共分支名（dev/test/preview/release）',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序（1-dev,2-test,3-preview,4-release）',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_env_id` (`env_id`),
    UNIQUE KEY `uk_project_env` (`project_id`, `env_code`),
    KEY `idx_project_id` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CVM环境表';

-- 分支登记表
CREATE TABLE `cvm_branch` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID（自增）',
    `branch_id` BIGINT NOT NULL COMMENT '分支ID（雪花算法生成，业务ID）',
    `project_id` BIGINT NOT NULL COMMENT '项目ID',
    `branch_name` VARCHAR(100) NOT NULL COMMENT '分支名',
    `branch_type` VARCHAR(30) NOT NULL COMMENT '分支类型（ENV_BRANCH/REQUIREMENT_BRANCH/MASTER）',
    `base_branch` VARCHAR(100) COMMENT '基于的分支',
    `source` VARCHAR(20) COMMENT '来源（CREATED新建/PULLED远程拉取）',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_branch_id` (`branch_id`),
    UNIQUE KEY `uk_project_branch` (`project_id`, `branch_name`),
    KEY `idx_project_id` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CVM分支登记表';

-- 需求表
CREATE TABLE `cvm_requirement` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID（自增）',
    `requirement_id` BIGINT NOT NULL COMMENT '需求ID（雪花算法生成，业务ID）',
    `project_id` BIGINT NOT NULL COMMENT '项目ID',
    `requirement_name` VARCHAR(100) NOT NULL COMMENT '需求名称',
    `requirement_url` VARCHAR(500) COMMENT '需求地址',
    `branch_name` VARCHAR(100) NOT NULL COMMENT '开发分支名',
    `branch_source` VARCHAR(20) NOT NULL COMMENT '分支来源（NEW新建/REMOTE远程拉取）',
    `creator_user_id` BIGINT NOT NULL COMMENT '创建人用户ID',
    `current_env` VARCHAR(20) NOT NULL COMMENT '当前所在环境（DEV/TEST/PREVIEW/RELEASE）',
    `status` VARCHAR(30) NOT NULL COMMENT '需求状态（DEVELOPING/CONFLICT/MERGED/RELEASED）',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除（0正常/1已删除）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_requirement_id` (`requirement_id`),
    KEY `idx_project_id` (`project_id`),
    KEY `idx_creator` (`creator_user_id`),
    KEY `idx_current_env` (`current_env`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CVM需求表';

-- 合并记录表
CREATE TABLE `cvm_merge_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID（自增）',
    `merge_id` BIGINT NOT NULL COMMENT '合并记录ID（雪花算法生成，业务ID）',
    `project_id` BIGINT NOT NULL COMMENT '项目ID',
    `requirement_id` BIGINT NOT NULL COMMENT '需求ID',
    `requirement_name` VARCHAR(100) COMMENT '需求名称',
    `branch_name` VARCHAR(100) NOT NULL COMMENT '要合并的开发分支',
    `user_id` BIGINT NOT NULL COMMENT '操作人用户ID',
    `target_env` VARCHAR(20) NOT NULL COMMENT '目标环境（DEV/TEST/PREVIEW/RELEASE）',
    `target_branch` VARCHAR(100) NOT NULL COMMENT '目标公共分支（dev/test/preview/release）',
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态（PENDING/CONFLICT/MERGED/REJECTED）',
    `conflict_files` TEXT COMMENT '冲突文件列表（JSON数组）',
    `conflict_detail` TEXT COMMENT '冲突详情',
    `resolve_steps` TEXT COMMENT '解决冲突步骤（给用户展示）',
    `merge_commit` VARCHAR(100) COMMENT '合并后的提交ID',
    `cr_required` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否需要CR审核（release环境=1）',
    `merge_time` DATETIME COMMENT '合并时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除（0正常/1已删除）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_merge_id` (`merge_id`),
    KEY `idx_project_id` (`project_id`),
    KEY `idx_requirement_id` (`requirement_id`),
    KEY `idx_target_env` (`target_env`),
    KEY `idx_status` (`status`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CVM合并记录表（待合并列表/已合并历史）';

-- CR审核表
CREATE TABLE `cvm_cr_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID（自增）',
    `cr_id` BIGINT NOT NULL COMMENT 'CR记录ID（雪花算法生成，业务ID）',
    `project_id` BIGINT NOT NULL COMMENT '项目ID',
    `requirement_id` BIGINT NOT NULL COMMENT '需求ID',
    `merge_id` BIGINT NOT NULL COMMENT '关联合并记录ID',
    `submitter_user_id` BIGINT COMMENT 'CR发起人用户ID',
    `reviewer_user_id` BIGINT NOT NULL COMMENT '被指定的评审人用户ID',
    `cr_status` VARCHAR(20) NOT NULL COMMENT 'CR状态（PENDING待审核/PASS通过/REJECT驳回）',
    `cr_comment` VARCHAR(500) COMMENT '审核意见',
    `cr_time` DATETIME COMMENT '审核时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除（0正常/1已删除）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_cr_id` (`cr_id`),
    KEY `idx_project_id` (`project_id`),
    KEY `idx_requirement_id` (`requirement_id`),
    KEY `idx_merge_id` (`merge_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CVM代码评审(CR)记录表';

-- 操作日志表
CREATE TABLE `cvm_operation_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID（自增）',
    `log_id` BIGINT NOT NULL COMMENT '日志ID（雪花算法生成，业务ID）',
    `project_id` BIGINT COMMENT '项目ID',
    `requirement_id` BIGINT COMMENT '需求ID',
    `merge_id` BIGINT COMMENT '合并记录ID',
    `operator_user_id` BIGINT COMMENT '操作人用户ID',
    `action` VARCHAR(50) NOT NULL COMMENT '操作类型',
    `detail` VARCHAR(1000) COMMENT '操作详情',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_log_id` (`log_id`),
    KEY `idx_project_id` (`project_id`),
    KEY `idx_requirement_id` (`requirement_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CVM操作日志表';
