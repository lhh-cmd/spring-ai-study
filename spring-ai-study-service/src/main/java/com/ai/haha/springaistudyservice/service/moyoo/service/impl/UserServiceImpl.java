package com.ai.haha.springaistudyservice.service.moyoo.service.impl;

import com.ai.haha.springaistudyservice.service.moyoo.dto.UserLoginDTO;
import com.ai.haha.springaistudyservice.service.moyoo.dto.UserRegisterDTO;
import com.ai.haha.springaistudyservice.service.moyoo.entity.User;
import com.ai.haha.springaistudyservice.service.moyoo.enums.UserStatus;
import com.ai.haha.springaistudyservice.service.moyoo.mapper.UserMapper;
import com.ai.haha.springaistudyservice.service.moyoo.service.UserService;
import com.ai.haha.springaistudyservice.service.moyoo.statemachine.UserStatusStateMachine;
import com.ai.haha.springaistudyservice.service.moyoo.util.SnowflakeIdGenerator;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 用户服务实现类
 */
@Service
public class UserServiceImpl implements UserService {
    
    @Resource
    private UserMapper userMapper;
    
    @Resource
    private SnowflakeIdGenerator idGenerator;
    
    @Override
    @Transactional
    public User register(UserRegisterDTO registerDTO) {
        // 检查用户名是否已存在
        if (userMapper.existsByUsername(registerDTO.getUsername())) {
            throw new RuntimeException("用户名已存在");
        }
        
        // 检查用户ID是否重复（理论上不会，但为了安全）
        Long userId;
        do {
            userId = idGenerator.nextId();
        } while (userMapper.existsByUserId(userId));
        
        User user = new User();
        user.setUserId(userId);
        user.setUsername(registerDTO.getUsername());
        user.setPassword(registerDTO.getPassword()); // 实际应该加密存储
        user.setProfession(registerDTO.getProfession());
        user.setDailyWorkHours(registerDTO.getDailyWorkHours());
        user.setStatus(UserStatus.OFFLINE.getCode()); // 默认离线
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        
        userMapper.insert(user);
        return user;
    }
    
    @Override
    @Transactional
    public User login(UserLoginDTO loginDTO) {
        User user = userMapper.selectByUsername(loginDTO.getUsername());
        if (user == null) {
            throw new RuntimeException("用户名或密码错误");
        }
        
        // 验证密码（实际应该使用加密比较）
        if (!user.getPassword().equals(loginDTO.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }
        
        // 使用状态机更新用户状态为在线
        // 如果用户当前状态是摸鱼中（可能是登录态过期导致的异常状态），先强制转为离线
        UserStatus currentStatus = UserStatus.fromCode(user.getStatus());
        if (currentStatus == UserStatus.MOYOOING) {
            // 如果用户正在摸鱼中，说明可能是异常状态，先强制转为离线
            user.setStatus(UserStatus.OFFLINE.getCode());
            user.setUpdateTime(LocalDateTime.now());
            userMapper.updateById(user);
            currentStatus = UserStatus.OFFLINE;
        }
        
        // 从离线状态转为在线
        UserStatus newStatus = UserStatusStateMachine.login(currentStatus);
        user.setStatus(newStatus.getCode());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
        
        return user;
    }
    
    @Override
    public User findById(Long userId) {
        User user = userMapper.selectByUserId(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        return user;
    }
    
    @Override
    public boolean existsByUsername(String username) {
        return userMapper.existsByUsername(username);
    }
    
    @Override
    @Transactional
    public void logout(Long userId) {
        User user = userMapper.selectByUserId(userId);
        if (user == null) {
            return; // 用户不存在，直接返回
        }
        
        // 使用状态机更新用户状态为离线
        UserStatus currentStatus = UserStatus.fromCode(user.getStatus());
        UserStatus newStatus = UserStatusStateMachine.logout(currentStatus);
        user.setStatus(newStatus.getCode());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
    }
    
    @Override
    @Transactional
    public void forceLogout(Long userId) {
        User user = userMapper.selectByUserId(userId);
        if (user == null) {
            return; // 用户不存在，直接返回
        }
        
        // 强制更新用户状态为离线（绕过状态机检查）
        user.setStatus(UserStatus.OFFLINE.getCode());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
    }
}

