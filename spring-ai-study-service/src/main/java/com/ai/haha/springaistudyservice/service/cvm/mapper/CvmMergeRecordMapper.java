package com.ai.haha.springaistudyservice.service.cvm.mapper;

import com.ai.haha.springaistudyservice.service.cvm.entity.CvmMergeRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * CVM合并记录Mapper
 */
@Mapper
public interface CvmMergeRecordMapper extends BaseMapper<CvmMergeRecord> {

    /**
     * 根据合并记录ID（业务ID）查找
     */
    @Select("SELECT * FROM cvm_merge_record WHERE merge_id = #{mergeId}")
    CvmMergeRecord selectByMergeId(Long mergeId);

    /**
     * 根据项目+目标环境查找合并记录（待合并和已合并分开查，按创建时间倒序）
     */
    @Select("SELECT * FROM cvm_merge_record WHERE project_id = #{projectId} AND target_env = #{targetEnv} ORDER BY create_time ASC")
    List<CvmMergeRecord> selectByProjectIdAndEnv(Long projectId, String targetEnv);

    /**
     * 统计目标环境已成功合并的记录数（用于模拟冲突检测）
     */
    @Select("SELECT COUNT(*) FROM cvm_merge_record WHERE project_id = #{projectId} AND target_env = #{targetEnv} AND status = 'MERGED'")
    long countMerged(Long projectId, String targetEnv);

    /**
     * 统计目标环境在指定时间之前已成功合并的记录数（用于模拟冲突检测）
     */
    @Select("SELECT COUNT(*) FROM cvm_merge_record WHERE project_id = #{projectId} AND target_env = #{targetEnv} AND status = 'MERGED' AND merge_time < #{time}")
    long countMergedBefore(Long projectId, String targetEnv, java.time.LocalDateTime time);
}