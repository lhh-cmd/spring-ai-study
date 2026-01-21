package com.ai.haha.springaistudyservice.service.moyoo.mapper;

import com.ai.haha.springaistudyservice.service.moyoo.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 用户Mapper
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
    
    /**
     * 根据用户名查找用户
     */
    @Select("SELECT * FROM moyoo_user WHERE username = #{username}")
    User selectByUsername(String username);
    
    /**
     * 根据用户ID（业务ID）查找用户
     */
    @Select("SELECT * FROM moyoo_user WHERE user_id = #{userId}")
    User selectByUserId(Long userId);
    
    /**
     * 检查用户名是否存在
     */
    @Select("SELECT COUNT(*) > 0 FROM moyoo_user WHERE username = #{username}")
    boolean existsByUsername(String username);
    
    /**
     * 检查用户ID（业务ID）是否存在
     */
    @Select("SELECT COUNT(*) > 0 FROM moyoo_user WHERE user_id = #{userId}")
    boolean existsByUserId(Long userId);
}

