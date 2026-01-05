package com.ai.haha.springaistudyservice.service;

/**
 * AI学习服务接口
 */
public interface AiStudyService {
    
    /**
     * 获取欢迎信息
     * @return 欢迎信息
     */
    String getWelcomeMessage();
    
    /**
     * 处理AI学习请求
     * @param message 用户消息
     * @return 处理结果
     */
    String processMessage(String message);

    String commonChat();
}

