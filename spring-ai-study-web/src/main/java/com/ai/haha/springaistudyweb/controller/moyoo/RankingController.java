package com.ai.haha.springaistudyweb.controller.moyoo;

import com.ai.haha.springaistudyservice.service.moyoo.dto.RankingDTO;
import com.ai.haha.springaistudyservice.service.moyoo.service.RankingService;
import jakarta.annotation.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 榜单控制器
 */
@RestController
@RequestMapping("/api/moyoo/ranking")
public class RankingController {

    @Resource
    private RankingService rankingService;
    
    /**
     * 获取摸鱼时长榜单
     */
    @GetMapping("/duration")
    public ResponseEntity<Map<String, Object>> getDurationRanking(
            @RequestParam(defaultValue = "week") String period) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<RankingDTO> ranking = rankingService.getDurationRanking(period);
            response.put("success", true);
            response.put("data", ranking);
            response.put("period", period);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 获取摸鱼次数榜单
     */
    @GetMapping("/count")
    public ResponseEntity<Map<String, Object>> getCountRanking(
            @RequestParam(defaultValue = "week") String period) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<RankingDTO> ranking = rankingService.getCountRanking(period);
            response.put("success", true);
            response.put("data", ranking);
            response.put("period", period);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 获取碳排放榜单
     */
    @GetMapping("/carbon")
    public ResponseEntity<Map<String, Object>> getCarbonRanking(
            @RequestParam(defaultValue = "week") String period) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<RankingDTO> ranking = rankingService.getCarbonRanking(period);
            response.put("success", true);
            response.put("data", ranking);
            response.put("period", period);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}

