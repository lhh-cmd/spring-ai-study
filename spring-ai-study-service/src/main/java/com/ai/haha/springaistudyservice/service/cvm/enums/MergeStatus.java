package com.ai.haha.springaistudyservice.service.cvm.enums;

/**
 * 合并记录状态枚举
 */
public enum MergeStatus {

    PENDING("PENDING", "待合并"),
    CONFLICT("CONFLICT", "冲突待解决"),
    MERGED("MERGED", "已合并"),
    REJECTED("REJECTED", "已驳回");

    private final String code;
    private final String description;

    MergeStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static MergeStatus fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (MergeStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }
}
