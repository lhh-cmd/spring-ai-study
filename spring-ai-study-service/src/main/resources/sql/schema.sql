-- 摸鱼了么(MoYoo)数据库表结构

-- 用户表
CREATE TABLE `moyoo_user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID（自增）',
    `user_id` BIGINT NOT NULL COMMENT '用户ID（雪花算法生成，业务ID）',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名（唯一）',
    `password` VARCHAR(255) NOT NULL COMMENT '密码',
    `profession` VARCHAR(50) COMMENT '职业',
    `daily_work_hours` DOUBLE COMMENT '日工作时长（小时）',
    `status` INT NOT NULL DEFAULT 0 COMMENT '用户状态（0-离线，1-在线，2-摸鱼中）',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_profession` (`profession`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 摸鱼记录表
CREATE TABLE `moyoo_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID（自增）',
    `record_id` BIGINT NOT NULL COMMENT '记录ID（雪花算法生成，业务ID）',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `start_time` DATETIME NOT NULL COMMENT '开始时间',
    `end_time` DATETIME COMMENT '结束时间',
    `duration_minutes` DOUBLE COMMENT '摸鱼时长（分钟，支持小数）',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_record_id` (`record_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_start_time` (`start_time`),
    KEY `idx_end_time` (`end_time`),
    KEY `idx_user_start_time` (`user_id`, `start_time`),
    KEY `idx_user_end_time` (`user_id`, `end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='摸鱼记录表';

-- 摸鱼成果表
CREATE TABLE `moyoo_result` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID（自增）',
    `result_id` BIGINT NOT NULL COMMENT '成果ID（雪花算法生成，业务ID）',
    `record_id` BIGINT NOT NULL COMMENT '记录ID（关联摸鱼记录）',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `duration_minutes` DOUBLE NOT NULL COMMENT '摸鱼时长（分钟，支持小数）',
    `carbon_reduction_grams` DOUBLE COMMENT '减少碳排放（克）',
    `life_extension_minutes` DOUBLE COMMENT '寿命增加（分钟）',
    `ai_report` TEXT COMMENT 'AI生成的报告内容',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_result_id` (`result_id`),
    UNIQUE KEY `uk_record_id` (`record_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_user_create_time` (`user_id`, `create_time`),
    KEY `idx_carbon_reduction` (`carbon_reduction_grams`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='摸鱼成果表';

