package com.ai.haha.springaistudyservice.service.moyoo.service;

import com.ai.haha.springaistudyservice.service.moyoo.dto.RankingDTO;

import java.util.List;

/**
 * 榜单服务接口
 */
public interface RankingService {
    
    /**
     * 获取摸鱼时长榜单（周/月/年）
     */
    List<RankingDTO> getDurationRanking(String period); // period: week, month, year
    
    /**
     * 获取摸鱼次数榜单（周/月/年）
     */
    List<RankingDTO> getCountRanking(String period);
    
    /**
     * 获取碳排放榜单（周/月/年）
     */
    List<RankingDTO> getCarbonRanking(String period);
}

