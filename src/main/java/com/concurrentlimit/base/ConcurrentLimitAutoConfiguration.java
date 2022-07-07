package com.concurrentlimit.base;


import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * stater配置注入
 * @return
 */
@Configuration
@ConditionalOnClass(value = ConcurrentLimitAspectjService.class)
public class ConcurrentLimitAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(ConcurrentLimitAspectjService.class)
    public ConcurrentLimitAspectjService concurrentLimitAspectjService() {
        return new ConcurrentLimitAspectjService();
    }

}
