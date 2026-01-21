package com.ai.haha.springaistudyservice.service.moyoo.mapper;

import com.ai.haha.springaistudyservice.service.moyoo.entity.MoYooResult;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

/**
 * 摸鱼成果Mapper
 */
@Mapper
public interface MoYooResultMapper extends BaseMapper<MoYooResult> {
    
    /**
     * 根据记录ID查找成果
     */
    @Select("SELECT * FROM moyoo_result WHERE record_id = #{recordId}")
    MoYooResult selectByRecordId(Long recordId);
    
    /**
     * 根据用户ID查找所有成果，按创建时间倒序
     */
    @Select("SELECT * FROM moyoo_result WHERE user_id = #{userId} ORDER BY create_time DESC")
    java.util.List<MoYooResult> selectByUserIdOrderByCreateTimeDesc(Long userId);
    
    /**
     * 根据用户ID和时间范围查找成果
     */
    @Select("SELECT * FROM moyoo_result WHERE user_id = #{userId} " +
            "AND create_time >= #{startTime} AND create_time < #{endTime} " +
            "ORDER BY create_time DESC")
    java.util.List<MoYooResult> selectByUserIdAndTimeRange(Long userId, LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 统计用户总减少碳排放（克）
     */
    @Select("SELECT COALESCE(SUM(carbon_reduction_grams), 0) FROM moyoo_result WHERE user_id = #{userId}")
    Double sumCarbonReductionByUserId(Long userId);
    
    /**
     * 根据用户ID和时间范围统计总减少碳排放
     */
    @Select("SELECT COALESCE(SUM(carbon_reduction_grams), 0) FROM moyoo_result " +
            "WHERE user_id = #{userId} AND create_time >= #{startTime} AND create_time < #{endTime}")
    Double sumCarbonReductionByUserIdAndTimeRange(Long userId, LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 统计所有用户累计减少碳排放（克）
     */
    @Select("SELECT COALESCE(SUM(carbon_reduction_grams), 0) FROM moyoo_result")
    Double sumTotalCarbonReduction();
    
    /**
     * 统计所有用户累计寿命增加（分钟）
     */
    @Select("SELECT COALESCE(SUM(life_extension_minutes), 0) FROM moyoo_result")
    Double sumTotalLifeExtension();
}

