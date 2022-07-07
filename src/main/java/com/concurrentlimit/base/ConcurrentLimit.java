package com.concurrentlimit.base;

import com.concurrentlimit.base.delayQueue.DelayExecuteService;
import com.concurrentlimit.base.delayQueue.LimitExecuteService;
import com.concurrentlimit.base.delayQueue.WindowPoint;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 限流注解
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ConcurrentLimit {


    /**
     * 需要限流的业务key 需要符合SpEL表达式规则设置
     * @return
     */
    String key();

    /**
     *  限流次数，阈值
     * @return
     */
    int limitCount();

    /**
     * 动态限流次数 需要符合SpEL表达式规则设置
     * <br>如果设置了动态次数且大于0，则会覆盖限流次数limitCount</>
     * @return
     */
    String dynamicLimitCount() default "";

    /**
     * 当前窗口坐标,默认使用固定窗口算法计算
     * @return
     */
    WindowPoint windowPoint();

    /**
     * 延迟任务策略
     * 默认是执行原来的方法，也可以指定，如发送汇总通知等
     * 该类需要实现接口LimitExecuteService
     * @return
     */
    Class<? extends DelayExecuteService> delayImpl() default DelayExecuteService.class;

    /**
     * 延迟任务策略参数
     * @return
     */
    String delayValue() default "";

    /**
     * 拒绝策略
     * 超过阈值后，默认是什么都不做，但是也可以指定，如计数、打日志等
     * 该类需要实现接口LimitExecuteService
     * @return
     */
    Class<? extends LimitExecuteService> limitImpl() default LimitExecuteService.class;

    /**
     * 拒绝策略参数
     * @return
     */
    String limitValue() default "";
}
