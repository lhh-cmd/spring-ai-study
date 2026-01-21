package com.ai.haha.springaistudyservice.service.moyoo.enums;

/**
 * 用户状态枚举
 */
public enum UserStatus {
    OFFLINE(0, "离线"),
    ONLINE(1, "在线"),
    MOYOOING(2, "摸鱼中");
    
    private final Integer code;
    private final String description;
    
    UserStatus(Integer code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public Integer getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * 根据code获取枚举
     */
    public static UserStatus fromCode(Integer code) {
        if (code == null) {
            return OFFLINE;
        }
        for (UserStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return OFFLINE;
    }
    
    /**
     * 根据描述获取枚举
     */
    public static UserStatus fromDescription(String description) {
        for (UserStatus status : values()) {
            if (status.description.equals(description)) {
                return status;
            }
        }
        return OFFLINE;
    }
}

