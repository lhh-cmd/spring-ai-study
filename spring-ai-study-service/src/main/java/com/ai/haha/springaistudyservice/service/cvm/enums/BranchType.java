package com.ai.haha.springaistudyservice.service.cvm.enums;

/**
 * 分支类型枚举
 */
public enum BranchType {

    MASTER("MASTER", "主分支"),
    ENV_BRANCH("ENV_BRANCH", "环境公共分支"),
    REQUIREMENT_BRANCH("REQUIREMENT_BRANCH", "需求开发分支");

    private final String code;
    private final String description;

    BranchType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
