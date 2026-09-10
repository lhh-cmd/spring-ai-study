package com.ai.haha.springaistudyservice.service.cvm.mapper;

import com.ai.haha.springaistudyservice.service.cvm.entity.CvmProject;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * CVM项目Mapper
 */
@Mapper
public interface CvmProjectMapper extends BaseMapper<CvmProject> {

    /**
     * 根据项目ID（业务ID）查找项目
     */
    @Select("SELECT * FROM cvm_project WHERE project_id = #{projectId}")
    CvmProject selectByProjectId(Long projectId);

    /**
     * 根据项目编号查找项目
     */
    @Select("SELECT * FROM cvm_project WHERE project_code = #{projectCode}")
    CvmProject selectByProjectCode(String projectCode);

    /**
     * 检查项目编号是否存在
     */
    @Select("SELECT COUNT(*) > 0 FROM cvm_project WHERE project_code = #{projectCode}")
    boolean existsByProjectCode(String projectCode);

    /**
     * 按创建时间倒序查找项目列表
     */
    @Select("SELECT * FROM cvm_project ORDER BY create_time DESC")
    java.util.List<CvmProject> selectAllOrderByCreateTime();
}