package com.ai.haha.springaistudyweb.controller.moyoo;

import com.ai.haha.springaistudyservice.service.moyoo.dto.MoYooReportDTO;
import com.ai.haha.springaistudyservice.service.moyoo.entity.MoYooRecord;
import com.ai.haha.springaistudyservice.service.moyoo.service.MoYooService;
import jakarta.annotation.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 摸鱼控制器
 */
@RestController
@RequestMapping("/api/moyoo")
public class MoYooController {
    
    @Resource
    private MoYooService moYooService;
    
    /**
     * 开始摸鱼
     */
    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> startMoYoo(@RequestParam Long userId) {
        Map<String, Object> response = new HashMap<>();
        try {
            MoYooRecord record = moYooService.startMoYoo(userId);
            response.put("success", true);
            response.put("message", "开始摸鱼！");
            response.put("data", record);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 结束摸鱼
     */
    @PostMapping("/end")
    public ResponseEntity<Map<String, Object>> endMoYoo(@RequestParam Long userId, @RequestParam Double durationMinutes) {
        Map<String, Object> response = new HashMap<>();
        try {
            MoYooReportDTO report = moYooService.endMoYoo(userId, durationMinutes);
            response.put("success", true);
            response.put("message", "摸鱼结束！");
            response.put("data", report);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 获取当前摸鱼状态
     */
    @GetMapping("/current")
    public ResponseEntity<Map<String, Object>> getCurrentMoYoo(@RequestParam Long userId) {
        Map<String, Object> response = new HashMap<>();
        MoYooRecord record = moYooService.getCurrentMoYoo(userId);
        if (record != null) {
            response.put("success", true);
            response.put("data", record);
        } else {
            response.put("success", false);
            response.put("message", "没有进行中的摸鱼记录");
        }
        return ResponseEntity.ok(response);
    }
}

