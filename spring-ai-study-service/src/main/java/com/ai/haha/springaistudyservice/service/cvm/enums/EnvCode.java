package com.ai.haha.springaistudyservice.service.cvm.enums;

/**
 * 环境编码枚举
 */
public enum EnvCode {

    DEV("DEV", "开发环境", "dev", 1),
    TEST("TEST", "测试环境", "test", 2),
    PREVIEW("PREVIEW", "预发环境", "preview", 3),
    RELEASE("RELEASE", "正式环境", "release", 4);

    private final String code;
    private final String description;
    private final String branchName;
    private final int sortOrder;

    EnvCode(String code, String description, String branchName, int sortOrder) {
        this.code = code;
        this.description = description;
        this.branchName = branchName;
        this.sortOrder = sortOrder;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public String getBranchName() {
        return branchName;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    /**
     * 根据code获取枚举
     */
    public static EnvCode fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (EnvCode envCode : values()) {
            if (envCode.code.equals(code)) {
                return envCode;
            }
        }
        return null;
    }

    /**
     * 获取下一个环境（dev->test->preview->release），当前已是release则返回null
     */
    public EnvCode next() {
        switch (this) {
            case DEV:
                return TEST;
            case TEST:
                return PREVIEW;
            case PREVIEW:
                return RELEASE;
            default:
                return null;
        }
    }
}
