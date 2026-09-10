package com.ai.haha.springaistudyservice.service.cvm.mapper;

import com.ai.haha.springaistudyservice.service.cvm.entity.CvmUser;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * CVM用户Mapper
 */
@Mapper
public interface CvmUserMapper extends BaseMapper<CvmUser> {

    /**
     * 根据用户ID（业务ID）查找用户
     */
    @Select("SELECT * FROM cvm_user WHERE user_id = #{userId}")
    CvmUser selectByUserId(Long userId);

    /**
     * 检查用户名是否存在
     */
    @Select("SELECT COUNT(*) > 0 FROM cvm_user WHERE username = #{username}")
    boolean existsByUsername(String username);
}