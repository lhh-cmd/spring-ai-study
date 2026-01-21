package com.ai.haha.springaistudyservice.service.moyoo.statemachine;

import com.ai.haha.springaistudyservice.service.moyoo.enums.UserStatus;

/**
 * 用户状态机
 * 管理用户状态的转换规则
 */
public class UserStatusStateMachine {
    
    /**
     * 检查状态转换是否合法
     * 
     * @param currentStatus 当前状态
     * @param targetStatus 目标状态
     * @return 是否允许转换
     */
    public static boolean canTransition(UserStatus currentStatus, UserStatus targetStatus) {
        if (currentStatus == null) {
            currentStatus = UserStatus.OFFLINE;
        }
        
        // 状态转换规则
        switch (currentStatus) {
            case OFFLINE:
                // 离线状态只能转换为在线
                return targetStatus == UserStatus.ONLINE;
                
            case ONLINE:
                // 在线状态可以转换为离线或摸鱼中
                return targetStatus == UserStatus.OFFLINE || targetStatus == UserStatus.MOYOOING;
                
            case MOYOOING:
                // 摸鱼中状态只能转换为在线
                return targetStatus == UserStatus.ONLINE;
                
            default:
                return false;
        }
    }
    
    /**
     * 执行状态转换
     * 
     * @param currentStatus 当前状态
     * @param targetStatus 目标状态
     * @return 转换后的状态
     * @throws IllegalStateException 如果状态转换不合法
     */
    public static UserStatus transition(UserStatus currentStatus, UserStatus targetStatus) {
        if (!canTransition(currentStatus, targetStatus)) {
            throw new IllegalStateException(
                String.format("不允许从状态 %s 转换到 %s", 
                    currentStatus != null ? currentStatus.getDescription() : "未知",
                    targetStatus.getDescription())
            );
        }
        return targetStatus;
    }
    
    /**
     * 登录：离线 -> 在线
     */
    public static UserStatus login(UserStatus currentStatus) {
        return transition(currentStatus, UserStatus.ONLINE);
    }
    
    /**
     * 开始摸鱼：在线 -> 摸鱼中
     */
    public static UserStatus startMoYoo(UserStatus currentStatus) {
        return transition(currentStatus, UserStatus.MOYOOING);
    }
    
    /**
     * 结束摸鱼：摸鱼中 -> 在线
     */
    public static UserStatus endMoYoo(UserStatus currentStatus) {
        return transition(currentStatus, UserStatus.ONLINE);
    }
    
    /**
     * 退出登录：在线/摸鱼中 -> 离线
     * 如果正在摸鱼，强制结束摸鱼并转为离线
     */
    public static UserStatus logout(UserStatus currentStatus) {
        if (currentStatus == null) {
            return UserStatus.OFFLINE;
        }
        
        // 如果正在摸鱼，先转为在线，再转为离线
        if (currentStatus == UserStatus.MOYOOING) {
            // 强制结束摸鱼，允许从摸鱼中直接转为离线（用于登录态过期等情况）
            return UserStatus.OFFLINE;
        }
        
        return transition(currentStatus, UserStatus.OFFLINE);
    }
    
    /**
     * 强制退出登录：任何状态 -> 离线
     * 用于登录态过期等异常情况，绕过状态机检查
     */
    public static UserStatus forceLogout(UserStatus currentStatus) {
        return UserStatus.OFFLINE;
    }
}

