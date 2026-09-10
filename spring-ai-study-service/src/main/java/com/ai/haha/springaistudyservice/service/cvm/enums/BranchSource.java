package com.ai.haha.springaistudyservice.service.cvm.enums;

/**
 * 分支来源枚举
 */
public enum BranchSource {

    NEW("NEW", "新建分支"),
    REMOTE("REMOTE", "从远程已有分支拉取");

    private final String code;
    private final String description;

    BranchSource(String code, String description) {
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
