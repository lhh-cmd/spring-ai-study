package com.ai.haha.springaistudyservice.service.cvm.mapper;

import com.ai.haha.springaistudyservice.service.cvm.entity.CvmEnvironment;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * CVM环境Mapper
 */
@Mapper
public interface CvmEnvironmentMapper extends BaseMapper<CvmEnvironment> {

    /**
     * 根据项目和env_code查找环境
     */
    @Select("SELECT * FROM cvm_environment WHERE project_id = #{projectId} AND env_code = #{envCode}")
    CvmEnvironment selectByProjectIdAndEnvCode(Long projectId, String envCode);

    /**
     * 根据项目查找环境列表（按排序）
     */
    @Select("SELECT * FROM cvm_environment WHERE project_id = #{projectId} ORDER BY sort_order ASC")
    List<CvmEnvironment> selectByProjectId(Long projectId);
}