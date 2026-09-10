package com.ai.haha.springaistudyservice.service.cvm.service;

import com.ai.haha.springaistudyservice.service.cvm.dto.CvmUserLoginDTO;
import com.ai.haha.springaistudyservice.service.cvm.dto.CvmUserRegisterDTO;
import com.ai.haha.springaistudyservice.service.cvm.entity.CvmUser;

/**
 * CVM用户服务接口
 */
public interface CvmUserService {

    /**
     * 用户注册
     */
    CvmUser register(CvmUserRegisterDTO dto);

    /**
     * 用户登录
     */
    CvmUser login(CvmUserLoginDTO dto);

    /**
     * 根据用户ID查询
     */
    CvmUser findById(Long userId);

    /**
     * 根据用户名查询
     */
    CvmUser findByUsername(String username);
}
