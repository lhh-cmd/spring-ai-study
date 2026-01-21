package com.ai.haha.springaistudyservice.service.moyoo.service.impl;

import com.ai.haha.springaistudyservice.service.moyoo.mapper.MoYooRecordMapper;
import com.ai.haha.springaistudyservice.service.moyoo.mapper.MoYooResultMapper;
import com.ai.haha.springaistudyservice.service.moyoo.mapper.UserMapper;
import com.ai.haha.springaistudyservice.service.moyoo.service.StatisticsService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

/**
 * 统计服务实现类
 */
@Service
public class StatisticsServiceImpl implements StatisticsService {
    
    @Resource
    private UserMapper userMapper;
    
    @Resource
    private MoYooRecordMapper recordMapper;
    
    @Resource
    private MoYooResultMapper resultMapper;
    
    @Override
    public Long getTotalUserCount() {
        return userMapper.selectCount(new QueryWrapper<>());
    }
    
    @Override
    public Double getTotalMoYooDuration() {
        Double total = recordMapper.sumTotalDuration();
        return total != null ? total : 0.0;
    }
    
    @Override
    public Double getTotalCarbonReduction() {
        Double total = resultMapper.sumTotalCarbonReduction();
        return total != null ? total : 0.0;
    }
    
    @Override
    public Double getTotalLifeExtension() {
        Double total = resultMapper.sumTotalLifeExtension();
        return total != null ? total : 0.0;
    }
}

