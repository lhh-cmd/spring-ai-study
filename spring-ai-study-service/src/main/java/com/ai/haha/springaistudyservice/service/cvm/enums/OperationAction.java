package com.ai.haha.springaistudyservice.service.cvm.enums;

/**
 * 操作日志类型枚举
 */
public enum OperationAction {

    REGISTER_USER("注册用户"),
    LOGIN_USER("用户登录"),
    CREATE_PROJECT("注册项目"),
    INIT_ENVIRONMENT("初始化环境"),
    CREATE_ENVIRONMENT("创建环境"),
    CREATE_REQUIREMENT("创建需求"),
    CREATE_BRANCH("创建分支"),
    PULL_BRANCH("拉取分支"),
    MERGE("合并到环境分支"),
    MERGE_CONFLICT("合并冲突"),
    RESOLVE_CONFLICT("解决冲突并重新合并"),
    NEXT_ENV("进入下一环境"),
    EXIT_INTEGRATION("退出集成"),
    CR_PASS("CR通过"),
    CR_REJECT("CR驳回"),
    REJECT_MERGE("驳回合并"),
    PUBLISH("发布到线上"),
    MERGE_MASTER("合并master"),
    RESET_ENV("环境重置");

    private final String description;

    OperationAction(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
