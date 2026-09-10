package com.ai.haha.springaistudyservice.service.cvm.enums;

/**
 * 需求状态枚举
 */
public enum RequirementStatus {

    DEVELOPING("DEVELOPING", "开发中/待合并"),
    CONFLICT("CONFLICT", "冲突待解决"),
    MERGED("MERGED", "已合并"),
    PUBLISHED("PUBLISHED", "已发布"),
    MERGED_MASTER("MERGED_MASTER", "已合并master");

    private final String code;
    private final String description;

    RequirementStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static RequirementStatus fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (RequirementStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }
}
