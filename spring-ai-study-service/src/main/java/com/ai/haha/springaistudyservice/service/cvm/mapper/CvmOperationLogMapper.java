package com.ai.haha.springaistudyservice.service.cvm.mapper;

import com.ai.haha.springaistudyservice.service.cvm.entity.CvmOperationLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * CVM操作日志Mapper
 */
@Mapper
public interface CvmOperationLogMapper extends BaseMapper<CvmOperationLog> {

    /**
     * 根据项目查找日志（按时间倒序）
     */
    @Select("SELECT * FROM cvm_operation_log WHERE project_id = #{projectId} ORDER BY create_time DESC")
    List<CvmOperationLog> selectByProjectId(Long projectId);

    /**
     * 查找全部日志（按时间倒序）
     */
    @Select("SELECT * FROM cvm_operation_log ORDER BY create_time DESC")
    List<CvmOperationLog> selectAllOrderByCreateTime();
}