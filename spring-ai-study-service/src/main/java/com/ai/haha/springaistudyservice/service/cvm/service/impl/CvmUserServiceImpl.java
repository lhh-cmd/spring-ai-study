package com.ai.haha.springaistudyservice.service.cvm.service.impl;

import com.ai.haha.springaistudyservice.service.cvm.dto.CvmUserLoginDTO;
import com.ai.haha.springaistudyservice.service.cvm.dto.CvmUserRegisterDTO;
import com.ai.haha.springaistudyservice.service.cvm.entity.CvmUser;
import com.ai.haha.springaistudyservice.service.cvm.enums.OperationAction;
import com.ai.haha.springaistudyservice.service.cvm.mapper.CvmUserMapper;
import com.ai.haha.springaistudyservice.service.cvm.service.CvmOperationLogService;
import com.ai.haha.springaistudyservice.service.cvm.service.CvmUserService;
import com.ai.haha.springaistudyservice.service.moyoo.util.SnowflakeIdGenerator;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * CVM用户服务实现
 */
@Service
public class CvmUserServiceImpl implements CvmUserService {

    @Resource
    private CvmUserMapper cvmUserMapper;

    @Resource
    private SnowflakeIdGenerator idGenerator;

    @Resource
    private CvmOperationLogService operationLogService;

    @Override
    @Transactional
    public CvmUser register(CvmUserRegisterDTO dto) {
        if (!StringUtils.hasText(dto.getUsername()) || !StringUtils.hasText(dto.getPassword())) {
            throw new RuntimeException("用户名和密码不能为空");
        }
        if (cvmUserMapper.existsByUsername(dto.getUsername())) {
            throw new RuntimeException("用户名已存在");
        }
        CvmUser user = new CvmUser();
        user.setUserId(idGenerator.nextId());
        user.setUsername(dto.getUsername());
        user.setPassword(dto.getPassword());
        user.setNickname(StringUtils.hasText(dto.getNickname()) ? dto.getNickname() : dto.getUsername());
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        cvmUserMapper.insert(user);
        operationLogService.record(null, null, null, user.getUserId(), OperationAction.REGISTER_USER, "注册用户：" + user.getUsername());
        return user;
    }

    @Override
    public CvmUser login(CvmUserLoginDTO dto) {
        CvmUser user = cvmUserMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CvmUser>()
                        .eq(CvmUser::getUsername, dto.getUsername()));
        if (user == null || !user.getPassword().equals(dto.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }
        operationLogService.record(null, null, null, user.getUserId(), OperationAction.LOGIN_USER, "用户登录：" + user.getUsername());
        return user;
    }

    @Override
    public CvmUser findById(Long userId) {
        CvmUser user = cvmUserMapper.selectByUserId(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        return user;
    }

    @Override
    public CvmUser findByUsername(String username) {
        return cvmUserMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CvmUser>()
                        .eq(CvmUser::getUsername, username));
    }
}
