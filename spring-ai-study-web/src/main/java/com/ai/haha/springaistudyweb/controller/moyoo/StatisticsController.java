package com.ai.haha.springaistudyweb.controller.moyoo;

import com.ai.haha.springaistudyservice.service.moyoo.service.StatisticsService;
import jakarta.annotation.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 统计控制器
 */
@RestController
@RequestMapping("/api/moyoo/statistics")
public class StatisticsController {
    
    @Resource
    private StatisticsService statisticsService;
    
    /**
     * 获取累计统计数据
     */
    @GetMapping("/total")
    public ResponseEntity<Map<String, Object>> getTotalStatistics() {
        Map<String, Object> response = new HashMap<>();
        
        Long totalUsers = statisticsService.getTotalUserCount();
        Double totalDuration = statisticsService.getTotalMoYooDuration();
        Double totalCarbon = statisticsService.getTotalCarbonReduction();
        Double totalLife = statisticsService.getTotalLifeExtension();
        
        response.put("success", true);
        response.put("data", Map.of(
            "totalUsers", totalUsers != null ? totalUsers : 0L,
            "totalDuration", totalDuration != null ? totalDuration : 0.0,
            "totalCarbonReduction", totalCarbon != null ? totalCarbon : 0.0,
            "totalLifeExtension", totalLife != null ? totalLife : 0.0
        ));
        
        return ResponseEntity.ok(response);
    }
}

