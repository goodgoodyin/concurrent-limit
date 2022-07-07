package com.concurrentlimit.base.redis;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * redis缓存配置
 */
@Configuration
public class RedisConfig {

    private final static String URL = "";

    @Bean
    public RedisServiceImpl getCommonRedisService() {
        return  RedisServiceFactory.getService(URL);
    }
}
