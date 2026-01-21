package com.ai.haha.springaistudyweb.controller.moyoo;

import com.ai.haha.springaistudyservice.service.moyoo.enums.Profession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 职业控制器
 */
@RestController
@RequestMapping("/api/moyoo/profession")
public class ProfessionController {
    
    /**
     * 获取所有职业列表
     */
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> getProfessionList() {
        Map<String, Object> response = new HashMap<>();
        List<Map<String, String>> professions = new ArrayList<>();
        
        for (Profession profession : Profession.values()) {
            Map<String, String> item = new HashMap<>();
            item.put("value", profession.getChineseName());
            item.put("label", profession.getChineseName());
            item.put("englishName", profession.getEnglishName());
            professions.add(item);
        }
        
        response.put("success", true);
        response.put("data", professions);
        return ResponseEntity.ok(response);
    }
}

