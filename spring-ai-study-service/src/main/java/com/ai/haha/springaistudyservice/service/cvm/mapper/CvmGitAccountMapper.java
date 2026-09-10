package com.ai.haha.springaistudyservice.service.cvm.mapper;

import com.ai.haha.springaistudyservice.service.cvm.entity.CvmGitAccount;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * CVM全局Git服务账号Mapper
 */
@Mapper
public interface CvmGitAccountMapper extends BaseMapper<CvmGitAccount> {

    /**
     * 查询启用的全局Git服务账号
     */
    @Select("SELECT * FROM cvm_git_account WHERE active = 1 ORDER BY id LIMIT 1")
    CvmGitAccount selectActive();
}
