package com.ai.haha.springaistudyservice.service.moyoo.service;

import com.ai.haha.springaistudyservice.service.moyoo.dto.UserLoginDTO;
import com.ai.haha.springaistudyservice.service.moyoo.dto.UserRegisterDTO;
import com.ai.haha.springaistudyservice.service.moyoo.entity.User;

/**
 * 用户服务接口
 */
public interface UserService {
    
    /**
     * 用户注册
     */
    User register(UserRegisterDTO registerDTO);
    
    /**
     * 用户登录
     */
    User login(UserLoginDTO loginDTO);
    
    /**
     * 根据用户ID查找用户
     */
    User findById(Long userId);
    
    /**
     * 检查用户名是否存在
     */
    boolean existsByUsername(String username);
    
    /**
     * 用户退出登录
     */
    void logout(Long userId);
    
    /**
     * 强制退出登录（用于登录态过期等情况）
     */
    void forceLogout(Long userId);
}

