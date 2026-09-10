package com.ai.haha.springaistudyservice.service.cvm.mapper;

import com.ai.haha.springaistudyservice.service.cvm.entity.CvmBranch;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * CVM分支登记Mapper
 */
@Mapper
public interface CvmBranchMapper extends BaseMapper<CvmBranch> {

    /**
     * 根据项目+分支名查找分支登记
     */
    @Select("SELECT * FROM cvm_branch WHERE project_id = #{projectId} AND branch_name = #{branchName}")
    CvmBranch selectByProjectIdAndBranchName(Long projectId, String branchName);

    /**
     * 根据项目查找分支列表
     */
    @Select("SELECT * FROM cvm_branch WHERE project_id = #{projectId} ORDER BY create_time ASC")
    List<CvmBranch> selectByProjectId(Long projectId);
}