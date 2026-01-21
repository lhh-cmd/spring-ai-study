package com.ai.haha.springaistudyservice.service.moyoo.service.impl;

import com.ai.haha.springaistudyservice.service.moyoo.entity.User;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 摸鱼AI服务
 */
@Service
public class MoYooAIService {
    
    @Resource
    private ChatClient chatClient;
    
    @Value("classpath:prompts/moyoo-report-prompt.txt")
    private org.springframework.core.io.Resource promptTemplate;
    
    /**
     * 生成摸鱼报告
     */
    public String generateReport(User user, double durationMinutes, double carbonReduction, double lifeExtension) {
        try {
            // 读取提示词模板
            String promptTemplateContent = StreamUtils.copyToString(
                    promptTemplate.getInputStream(), StandardCharsets.UTF_8);
            
            // 替换模板变量（时长保留2位小数）
            String prompt = promptTemplateContent
                    .replace("${username}", user.getUsername() != null ? user.getUsername() : "用户")
                    .replace("${profession}", user.getProfession() != null ? user.getProfession() : "未知职业")
                    .replace("${durationMinutes}", String.format("%.2f", durationMinutes))
                    .replace("${carbonReduction}", String.format("%.2f", carbonReduction))
                    .replace("${lifeExtension}", String.format("%.2f", lifeExtension));
            
            // 调用AI生成报告
            return chatClient.call(prompt);
            
        } catch (IOException e) {
            // 如果读取模板失败，使用默认提示词
            String defaultPrompt = String.format(
                    "用户%s（职业：%s）本次摸鱼%d分钟，减少碳排放%.2f克，寿命增加%.2f分钟。请生成一份有趣的摸鱼报告，鼓励用户继续摸鱼。",
                    user.getUsername(), user.getProfession(), durationMinutes, carbonReduction, lifeExtension
            );
            return chatClient.call(defaultPrompt);
        }
    }
}

