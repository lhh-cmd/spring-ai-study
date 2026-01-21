package com.ai.haha.springaistudyservice.service.moyoo.util;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import org.springframework.stereotype.Component;

/**
 * 雪花算法ID生成器
 */
@Component
public class SnowflakeIdGenerator {
    
    private final Snowflake snowflake;
    
    public SnowflakeIdGenerator() {
        // 使用Hutool的雪花算法，workerId和datacenterId可以根据实际情况配置
        // 这里使用默认值，实际生产环境应该从配置文件读取
        this.snowflake = IdUtil.getSnowflake(1, 1);
    }
    
    /**
     * 生成下一个ID
     */
    public Long nextId() {
        return snowflake.nextId();
    }
}

