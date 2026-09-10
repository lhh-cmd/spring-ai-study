package com.ai.haha.springaistudyservice.service.cvm.enums;

/**
 * 代码评审(CR)结果枚举
 */
public enum CrStatus {

    PENDING("PENDING", "待审核"),
    PASS("PASS", "通过"),
    REJECT("REJECT", "驳回");

    private final String code;
    private final String description;

    CrStatus(String code, String description) {
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
