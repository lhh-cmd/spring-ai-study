package com.ai.haha.springaistudyservice.service.study.impl;

import com.ai.haha.springaistudyservice.service.study.AiStudyService;
import org.springframework.ai.chat.ChatClient;
import org.springframework.stereotype.Service;

/**
 * AI学习服务实现类
 */
@Service
public class AiStudyServiceImpl implements AiStudyService {

    @Override
    public String getWelcomeMessage() {
        return "欢迎使用Spring AI学习项目！";
    }
    
    @Override
    public String processMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            return "请输入有效的消息";
        }
        return "您发送的消息是: " + message + "，已成功处理！";
    }

    @Override
    public String commonChat() {
        return null;
    }
}

