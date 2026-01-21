package com.ai.haha.springaistudyservice.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus配置类
 */
@Configuration
@MapperScan("com.ai.haha.springaistudyservice.service.moyoo.mapper")
public class MyBatisPlusConfig {
    // MyBatis-Plus 分页插件已通过自动配置加载
    // 如需自定义分页插件，可以在这里添加 @Bean 方法
}

