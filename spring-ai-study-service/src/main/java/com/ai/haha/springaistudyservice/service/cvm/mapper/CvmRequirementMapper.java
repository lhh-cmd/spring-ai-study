package com.ai.haha.springaistudyservice.service.cvm.mapper;

import com.ai.haha.springaistudyservice.service.cvm.entity.CvmRequirement;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * CVM需求Mapper
 */
@Mapper
public interface CvmRequirementMapper extends BaseMapper<CvmRequirement> {

    /**
     * 根据需求ID（业务ID）查找需求
     */
    @Select("SELECT * FROM cvm_requirement WHERE requirement_id = #{requirementId} AND deleted = 0")
    CvmRequirement selectByRequirementId(Long requirementId);

    /**
     * 根据项目查找需求列表（按创建时间倒序）
     */
    @Select("SELECT * FROM cvm_requirement WHERE project_id = #{projectId} AND deleted = 0 ORDER BY create_time DESC")
    List<CvmRequirement> selectByProjectId(Long projectId);

    /**
     * 根据创建人查找需求列表（按创建时间倒序）
     */
    @Select("SELECT * FROM cvm_requirement WHERE creator_user_id = #{creatorUserId} AND deleted = 0 ORDER BY create_time DESC")
    List<CvmRequirement> selectByCreatorUserId(Long creatorUserId);
}