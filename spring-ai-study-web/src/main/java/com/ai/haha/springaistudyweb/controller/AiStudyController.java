package com.ai.haha.springaistudyweb.controller;

import com.ai.haha.springaistudyservice.service.AiStudyService;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * AI学习控制器
 */
@RestController
@RequestMapping("/api/ai-study")
@RequiredArgsConstructor
public class AiStudyController {

    @Resource
    private AiStudyService aiStudyService;
    
    /**
     * 获取欢迎信息
     */
    @GetMapping("/welcome")
    public String welcome() {
        return aiStudyService.getWelcomeMessage();
    }
    
    /**
     * 处理消息
     */
    @PostMapping("/message")
    public String processMessage(@RequestBody MessageRequest request) {
        return aiStudyService.processMessage(request.getMessage());
    }
    
    /**
     * 消息请求对象
     */
    public static class MessageRequest {
        private String message;
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
    }
}

