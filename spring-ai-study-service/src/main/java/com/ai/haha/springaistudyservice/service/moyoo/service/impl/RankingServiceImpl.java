package com.ai.haha.springaistudyservice.service.moyoo.service.impl;

import com.ai.haha.springaistudyservice.service.moyoo.dto.RankingDTO;
import com.ai.haha.springaistudyservice.service.moyoo.entity.MoYooRecord;
import com.ai.haha.springaistudyservice.service.moyoo.entity.User;
import com.ai.haha.springaistudyservice.service.moyoo.mapper.MoYooRecordMapper;
import com.ai.haha.springaistudyservice.service.moyoo.mapper.MoYooResultMapper;
import com.ai.haha.springaistudyservice.service.moyoo.mapper.UserMapper;
import com.ai.haha.springaistudyservice.service.moyoo.service.RankingService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 榜单服务实现类
 */
@Service
public class RankingServiceImpl implements RankingService {
    
    @Resource
    private MoYooRecordMapper recordMapper;
    
    @Resource
    private MoYooResultMapper resultMapper;
    
    @Resource
    private UserMapper userMapper;
    
    @Override
    public List<RankingDTO> getDurationRanking(String period) {
        LocalDateTime[] timeRange = getTimeRange(period);
        
        return userMapper.selectList(null).stream()
                .map(user -> {
                    List<MoYooRecord> records = recordMapper.selectByUserIdAndTimeRange(
                            user.getUserId(), timeRange[0], timeRange[1]
                    );
                    Double totalDuration = records.stream()
                            .filter(r -> r.getDurationMinutes() != null)
                            .mapToDouble(MoYooRecord::getDurationMinutes)
                            .sum();
                    
                    RankingDTO dto = new RankingDTO();
                    dto.setUserId(user.getUserId());
                    dto.setUsername(user.getUsername());
                    dto.setProfession(user.getProfession());
                    dto.setValue(totalDuration);
                    return dto;
                })
                .filter(dto -> dto.getValue() != null && dto.getValue() > 0)
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(100)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<RankingDTO> getCountRanking(String period) {
        LocalDateTime[] timeRange = getTimeRange(period);
        
        return userMapper.selectList(null).stream()
                .map(user -> {
                    List<MoYooRecord> records = recordMapper.selectByUserIdAndTimeRange(
                            user.getUserId(), timeRange[0], timeRange[1]
                    );
                    Double count = (double) records.size();
                    
                    RankingDTO dto = new RankingDTO();
                    dto.setUserId(user.getUserId());
                    dto.setUsername(user.getUsername());
                    dto.setProfession(user.getProfession());
                    dto.setValue(count);
                    return dto;
                })
                .filter(dto -> dto.getValue() != null && dto.getValue() > 0)
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(100)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<RankingDTO> getCarbonRanking(String period) {
        LocalDateTime[] timeRange = getTimeRange(period);
        
        return userMapper.selectList(null).stream()
                .map(user -> {
                    Double totalCarbon = resultMapper.sumCarbonReductionByUserIdAndTimeRange(
                            user.getUserId(), timeRange[0], timeRange[1]
                    );
                    
                    RankingDTO dto = new RankingDTO();
                    dto.setUserId(user.getUserId());
                    dto.setUsername(user.getUsername());
                    dto.setProfession(user.getProfession());
                    dto.setCarbonReduction(totalCarbon);
                    return dto;
                })
                .filter(dto -> dto.getCarbonReduction() != null && dto.getCarbonReduction() > 0)
                .sorted((a, b) -> Double.compare(b.getCarbonReduction(), a.getCarbonReduction()))
                .limit(100)
                .collect(Collectors.toList());
    }
    
    /**
     * 根据周期获取时间范围
     */
    private LocalDateTime[] getTimeRange(String period) {
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime;
        
        switch (period.toLowerCase()) {
            case "week":
                startTime = endTime.minusWeeks(1);
                break;
            case "month":
                startTime = endTime.minusMonths(1);
                break;
            case "year":
                startTime = endTime.minusYears(1);
                break;
            default:
                startTime = endTime.minusWeeks(1); // 默认周
        }
        
        return new LocalDateTime[]{startTime, endTime};
    }
}

