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
@ConditionalOnClass(value = com.concurrentlimit.base.ConcurrentLimitAspectj.class)
public class ConcurrentLimitAspectAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(com.concurrentlimit.base.ConcurrentLimitAspectj.class)
    public com.concurrentlimit.base.ConcurrentLimitAspectj concurrentLimitAspectjService() {
        return new com.concurrentlimit.base.ConcurrentLimitAspectj();
    }

}
