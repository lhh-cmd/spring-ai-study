package com.ai.haha.springaistudyservice.service.cvm.mapper;

import com.ai.haha.springaistudyservice.service.cvm.entity.CvmCrRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * CVM代码评审(CR)记录Mapper
 */
@Mapper
public interface CvmCrRecordMapper extends BaseMapper<CvmCrRecord> {

    /**
     * 根据关联合并记录ID查找所有CR记录
     */
    @Select("SELECT * FROM cvm_cr_record WHERE merge_id = #{mergeId} ORDER BY create_time ASC")
    List<CvmCrRecord> selectByMergeId(Long mergeId);

    /**
     * 判断指定合并记录是否已通过CR
     */
    @Select("SELECT COUNT(*) > 0 FROM cvm_cr_record WHERE merge_id = #{mergeId} AND cr_status = 'PASS'")
    boolean hasPassed(Long mergeId);
}