package com.ai.haha.springaistudyservice.service.moyoo.service;

import com.ai.haha.springaistudyservice.service.moyoo.dto.MoYooReportDTO;
import com.ai.haha.springaistudyservice.service.moyoo.entity.MoYooRecord;

/**
 * 摸鱼服务接口
 */
public interface MoYooService {
    
    /**
     * 开始摸鱼
     */
    MoYooRecord startMoYoo(Long userId);
    
    /**
     * 结束摸鱼
     * @param userId 用户ID
     * @param durationMinutes 摸鱼时长（分钟，支持小数），由前端计算并提交
     */
    MoYooReportDTO endMoYoo(Long userId, Double durationMinutes);
    
    /**
     * 获取当前进行中的摸鱼记录
     */
    MoYooRecord getCurrentMoYoo(Long userId);
}

