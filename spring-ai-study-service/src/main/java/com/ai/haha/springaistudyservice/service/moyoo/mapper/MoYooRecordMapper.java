package com.ai.haha.springaistudyservice.service.moyoo.mapper;

import com.ai.haha.springaistudyservice.service.moyoo.entity.MoYooRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 摸鱼记录Mapper
 */
@Mapper
public interface MoYooRecordMapper extends BaseMapper<MoYooRecord> {
    
    /**
     * 根据用户ID查找所有记录，按开始时间倒序
     */
    @Select("SELECT * FROM moyoo_record WHERE user_id = #{userId} ORDER BY start_time DESC")
    List<MoYooRecord> selectByUserIdOrderByStartTimeDesc(Long userId);
    
    /**
     * 查找用户未结束的记录
     */
    @Select("SELECT * FROM moyoo_record WHERE user_id = #{userId} AND end_time IS NULL ORDER BY start_time DESC LIMIT 1")
    MoYooRecord selectFirstByUserIdAndEndTimeIsNull(Long userId);
    
    /**
     * 根据用户ID和时间范围查找记录
     */
    @Select("SELECT * FROM moyoo_record WHERE user_id = #{userId} " +
            "AND start_time >= #{startTime} AND start_time < #{endTime} " +
            "ORDER BY start_time DESC")
    List<MoYooRecord> selectByUserIdAndTimeRange(Long userId, LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 统计用户摸鱼总时长（分钟，支持小数）
     */
    @Select("SELECT COALESCE(SUM(duration_minutes), 0) FROM moyoo_record " +
            "WHERE user_id = #{userId} AND end_time IS NOT NULL")
    Double sumDurationByUserId(Long userId);
    
    /**
     * 统计用户摸鱼次数
     */
    @Select("SELECT COUNT(*) FROM moyoo_record WHERE user_id = #{userId} AND end_time IS NOT NULL")
    Long countByUserIdAndEndTimeIsNotNull(Long userId);
    
    /**
     * 统计所有用户累计摸鱼时长（分钟，支持小数）
     */
    @Select("SELECT COALESCE(SUM(duration_minutes), 0) FROM moyoo_record WHERE end_time IS NOT NULL")
    Double sumTotalDuration();
}

