package com.ai.haha.springaistudyservice.service.moyoo.service;

/**
 * 统计服务接口
 */
public interface StatisticsService {
    
    /**
     * 获取累计用户注册量
     */
    Long getTotalUserCount();
    
    /**
     * 获取累计摸鱼时长（分钟，支持小数）
     */
    Double getTotalMoYooDuration();
    
    /**
     * 获取累计减少碳排放（克）
     */
    Double getTotalCarbonReduction();
    
    /**
     * 获取累计寿命增加（分钟）
     */
    Double getTotalLifeExtension();
}

