package com.concurrentlimit.base;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 限流切面
 * <br><默认扫描全部包，如果包下类太多
 * <br>需要设置Pointcut，可以配置 @SpringBootApplication(exclude = { ConcurrentLimitAspectAutoConfiguration.class})
 * <br>自己实现一个ConcurrentLimitAspectj，覆盖此类
 */
@Component
@Aspect
public class ConcurrentLimitAspectj {

    @Autowired
    private ConcurrentLimitAspectjService concurrentLimitAspectjService;

    @Pointcut("@annotation(com.concurrentlimit.base.annotation.ConcurrentLimit)")
    private void currentLimit() {

    }

    /**
     * 环绕执行，如果被拦截了就不用执行了
     *
     * @param joinPoint
     */
    @Around("currentLimit()")
    public void currentLimit(ProceedingJoinPoint joinPoint) throws Throwable {
        concurrentLimitAspectjService.currentLimit(joinPoint);
    }

}
