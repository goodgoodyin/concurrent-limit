package com.concurrentlimit.base;


import com.concurrentlimit.base.annotation.ConcurrentLimit;
import com.concurrentlimit.base.delayQueue.DelayExecuteService;
import com.concurrentlimit.base.delayQueue.LimitExecuteService;
import com.concurrentlimit.base.delayQueue.WindowPoint;
import com.concurrentlimit.base.redis.RedisServiceImpl;
import com.concurrentlimit.base.utils.AsyncUtils;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 限流延迟算法AOP
 */
@Service
public class ConcurrentLimitAspectjService implements ApplicationListener, ApplicationContextAware {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConcurrentLimitAspectjService.class);

    private static final  ExpressionParser parser = new SpelExpressionParser(); // 创建解析器

    private static final DelayQueue<DelayVO> queue = new DelayQueue<>();    // 延迟队列

    private AtomicInteger DELAY_TIME_MAX = new AtomicInteger(0);  // 等待关闭最大时间

    private Integer MAX_WAIT_TIME = 60;     // 最大等待时间为60s

    private static ApplicationContext APP_CONTEXT;

    @Autowired
    private RedisServiceImpl redisService;

    /**
     * 环绕执行，如果被拦截了就不用执行了
     *
     * @param joinPoint
     */
    public void currentLimit(ProceedingJoinPoint joinPoint) throws Throwable {
        LOGGER.info("currentLimit start");
        MethodSignature signature = (MethodSignature)joinPoint.getSignature();
        Method method = signature.getMethod();

        Parameter[] parameters = method.getParameters();
        if (null == parameters || 0 == parameters.length) {
            LOGGER.error("parameters can not null");
            throw new RuntimeException("parameters can not null");
        }

        ConcurrentLimit concurrentLimit = method.getAnnotation(ConcurrentLimit.class);
        if (null == concurrentLimit) {
            return;
        }

        // SpEL解析Key
        String key = method.getName() + ":" + parseExpressionSpEL(concurrentLimit.key(), joinPoint, parameters);

        // 延迟执行操作，默认调用拦截的方法
        Runnable runnable;
        if (!concurrentLimit.delayImpl().equals(DelayExecuteService.class)) {
            // 自定义执行操作
            runnable = () -> (APP_CONTEXT.getBean(concurrentLimit.delayImpl()))
                    .delayExecute(parseExpressionSpEL(concurrentLimit.delayValue(), joinPoint, parameters));
        } else {
            runnable = () -> {
                try {
                    joinPoint.proceed();
                } catch (Throwable throwable) {
                    LOGGER.error("currentLimit throwable", throwable);
                }
            };
        }

        if (null == runnable) {
            LOGGER.error("runnable is null");
        }

        // 获取限流次数
        int limitCount = genLimitCount(concurrentLimit.limitCount(), concurrentLimit.dynamicLimitCount(),
                joinPoint, parameters);

        // 限流判断
        boolean check = check(key, limitCount, runnable, concurrentLimit.windowPoint());
        if (!check) {
            LOGGER.info("exceed threshold");
            // 限流后执行的操作
            if (!concurrentLimit.limitImpl().equals(LimitExecuteService.class)) {
                (APP_CONTEXT.getBean(concurrentLimit.limitImpl()))
                        .limitExecute(parseExpressionSpEL(concurrentLimit.limitValue(), joinPoint, parameters));
            }
            return;
        }

        joinPoint.proceed();
    }

    /**
     * 获取限流次数
     * @param limitCount
     * @param activeLimitCount
     * @param joinPoint
     * @param parameters
     * @return
     */
    private int genLimitCount(int limitCount, String activeLimitCount, ProceedingJoinPoint joinPoint,
                            Parameter[] parameters) {
        if (StringUtils.isEmpty(activeLimitCount)) {
            return limitCount;
        }
        int activeCount = 0;
        try {
            Object spELCount = parseExpressionSpEL(activeLimitCount, joinPoint, parameters);
            if (null == spELCount) {
                return limitCount;
            }
            activeCount = (int)spELCount;
        } catch (Exception e) {
            LOGGER.error("genLimitCount error ", e);
        }

        return 0 == activeCount
                ? limitCount : activeCount;
    }

    /**
     * 解析SpEL
     * @param spELStr
     * @param joinPoint
     * @param parameters
     * @return
     */
    private Object parseExpressionSpEL(String spELStr, ProceedingJoinPoint joinPoint, Parameter[] parameters) {

        // 解析表达式
        Expression expression = parser.parseExpression(spELStr);
        // 构造上下文
        EvaluationContext context = new StandardEvaluationContext(joinPoint.getTarget());
        // set值
        Object[] args = joinPoint.getArgs();
        for (int i = 0; i < parameters.length; i ++) {
            context.setVariable(parameters[i].getName(), args[i]);
        }
        return expression.getValue(context);
    }

    /**
     * 查询并更新限制次数
     */
    private static final String limitFlagScript =
            "-- lua脚本\n" +
                    "local key_name = KEYS[1] \n" +
                    "-- 超时时间\n" +
                    "local expire_time = ARGV[1] \n" +
                    "-- 限制次数\n" +
                    "local limit_count = ARGV[2] \n" +
                    "local field = 'count'\n" +
                    "local redis_value = redis.call('hget', key_name, field)\n" +
                    "if redis_value then\n" +
                    "    if tonumber(redis_value) >= tonumber(limit_count) then\n" +
                    "        return 'limit'  \n" +
                    "    end   \n" +
                    "    redis.call('hset', key_name, field, tonumber(redis_value) + 1)\n" +
                    "    return 'continue'\n" +
                    "end    \n" +
                    "redis.call('hset', key_name, field, 1)\n" +
                    "redis.call('EXPIRE', key_name, expire_time)\n" +
                    "return 'continue'\n";

    /**
     * 查询并更新是否加入延迟队列flag
     */
    private static final String flagScript =
            "-- lua脚本\n" +
                    "local key_name = KEYS[1] \n" +
                    "-- 超时时间\n" +
                    "local expire_time = ARGV[1] \n" +
                    "local field = 'flag'\n" +
                    "local redis_value = redis.call('hget', key_name, field)\n" +
                    "if redis_value then\n" +
                    "    return redis_value\n" +
                    "end   \n" +
                    "redis.call('hset', key_name, field, 1)\n" +
                    "redis.call('EXPIRE', key_name, expire_time)\n" +
                    "return nil\n";

    /**
     * 是否需要限流
     * @param key           业务id
     * @param limitCount    限流次数，阈值
     * @param runnable      执行方法
     * @param windowPoint   时间窗口坐标
     * @return
     */
    public boolean check(String key, int limitCount, Runnable runnable, WindowPoint windowPoint) {

        if (!valid(limitCount, runnable)) {
            throw new RuntimeException("param error");
        }

        Integer sectionTime = windowPoint.getSectionTime();

        // 更新线程关闭最大等待时间
        DELAY_TIME_MAX.getAndUpdate((time) -> sectionTime > DELAY_TIME_MAX.get() && sectionTime <= MAX_WAIT_TIME
                ? sectionTime : DELAY_TIME_MAX.get());

        String redisKey = "CURRENT_LIMIT:" + key + "_" + windowPoint.getPoint();
        LOGGER.info("check key {}", redisKey);

        // redis过期时间为窗口时间+1,防止窗口提前失效
        Integer expireTime = sectionTime + 1;

        // 是否到达请求阈值
        boolean limitFlag = getLimitFlag(redisKey, expireTime, String.valueOf(limitCount));

        // 单位时间内没有到达请求阈值
        if (limitFlag) {
            return true;
        }

        LOGGER.info("check {} count greater limitCount {}", redisKey, limitCount);

        // 是否加入延迟队列
        boolean delayQueueFlag = getDelayQueueFlag(redisKey, expireTime);
        if (!delayQueueFlag) {
            LOGGER.info("check {} already add into delayQueue", redisKey);
            return false;
        }

        // 加入延迟队列
        DelayVO delayVO = new DelayVO(sectionTime, runnable);
        LOGGER.info("delay task estimated execution time {} ", DateTime.now().plusSeconds(delayVO.delayTime.intValue()));
        queue.put(delayVO);
        return false;
    }

    /**
     * 参数校验
     * @param limitCount
     * @param runnable
     * @return
     */
    private boolean valid(int limitCount, Runnable runnable) {
        if (null == runnable) {
            LOGGER.error("runnable must not null");
            return false;
        }

        if (limitCount <= 0) {
            LOGGER.error("limitCount must greater one");
            return false;
        }

        return true;

    }

    /**
     * 查询是否达到限制次数。true：未到达。false：已经到达，需要限流
     * @param key
     * @param expireTime
     * @param limitCount
     * @return
     */
    private boolean getLimitFlag(String key, Integer expireTime, String limitCount) {
        List<String> keys = Arrays.asList(key);
        List<String> args = Arrays.asList(String.valueOf(expireTime), limitCount);
        Object obj = redisService.eval(limitFlagScript, keys, args);
        if (null == obj) {
            LOGGER.error("getLimitFlag error");
        }

        String limitFlag = (String) obj;
        if ("continue".equals(limitFlag)) {
            return  true;
        }
        return false;
    }

    /**
     * 判断是否加入延迟队列。true:key还未加入延迟队列，false:已经加入到延迟队列中
     * @param key
     * @param expireTime
     * @return
     */
    private boolean getDelayQueueFlag(String key, Integer expireTime) {

        List<String> keys = Arrays.asList(key);
        List<String> args = Arrays.asList(String.valueOf(expireTime));

        Object flag = redisService.eval(flagScript, keys, args);

        LOGGER.info("getDelayQueueFlag flag {} time {} res {}", flag, DateTime.now(), null == flag);
        return null == flag;
    }

    /**
     * 获取延迟队列size
     * @return
     */
    private int queueSize() {
        if (null == queue) {
            return 0;
        }
        return queue.size();
    }

    /**
     * 执行监听
     * @param event
     */
    @Override
    public void onApplicationEvent(ApplicationEvent event) {

        if (event instanceof ContextRefreshedEvent) {
            onApplicationStartEvent((ContextRefreshedEvent) event);
        }

        if (event instanceof ContextClosedEvent) {
            onApplicationCloseEvent((ContextClosedEvent) event);
        }
    }

    /**
     * 这里注意实现的是ContextRefreshedEvent，
     * 这里要做空判断，才执行event方法，不然会重复加载
     * 启动延迟队列
     *
     * @param event
     */
    private void onApplicationStartEvent(ContextRefreshedEvent event) {
        if (null == event.getApplicationContext().getParent()) {
            LOGGER.info("SpringContextStartedService onApplicationEvent");
            new Thread(() -> execute(), "THREAD-CURRENT-LIMIT").start();
        }
    }

    /**
     * 容器关闭的时候执行
     * 判断延迟队列中的任务是否执行完
     * @param contextClosedEvent
     */
    private void onApplicationCloseEvent(ContextClosedEvent contextClosedEvent) {

        LOGGER.info("onApplicationCloseEvent start delayQueue size {}", queueSize());
        // 延迟队列为空直接返回队列
        if (0 == queueSize()) {
            LOGGER.info("onApplicationEvent delayQueue empty");
            return;
        }

        // 循环等待延迟队列中的任务执行完
        // 最大等待时间为全部任务中的最大延迟时间
        DateTime dateTime = DateTime.now().plusSeconds(DELAY_TIME_MAX.get());
        while (queueSize() > 0) {
            if (dateTime.isBeforeNow()) {
                LOGGER.error("onApplicationEvent stop max time out {}", DELAY_TIME_MAX);
                break;
            }

            try {
                Thread.sleep(500); // 等待0.5s,让判断延迟队列有没有执行完成不要刷那么快
            } catch (InterruptedException e) {
                LOGGER.error("InterruptedException ", e);
            }
            LOGGER.info("in circulation delayQueue size {}", queueSize());
        }
        LOGGER.info("circle end delayQueue size {}", queueSize());

        // 等待3s,给队列里的任务一点执行实现
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            LOGGER.error("InterruptedException ", e);
        }

        LOGGER.info("onApplicationCloseEvent end delayQueue size {}", queueSize());
    }

    /**
     * 循环执行延迟队列，从启动一直占用一个线程
     */
    private static void execute() {

        LOGGER.info("execute start");
        while (true) {
            try {
                // 等到队列里面有时间到了的任务就take出来执行
                DelayVO take = queue.take();
                // 异步执行队列里的任务
                AsyncUtils.run(take.runnable);
            } catch (InterruptedException e) {
                LOGGER.error("InterruptedException ", e);
            }
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        APP_CONTEXT = applicationContext;
    }

    /**
     * 延迟队列对象
     */
    private static class DelayVO implements Delayed {

        private Long timeout;                // 延迟时间 （NANOSECONDS）
        private Runnable runnable;           // 执行函数
        private Integer delayTime;           // 延迟时间（s）


        public DelayVO(Integer delayTime, Runnable runnable) {
            this.delayTime = delayTime;
            this.timeout = TimeUnit.NANOSECONDS.convert(Long.valueOf(delayTime), TimeUnit.SECONDS) + System.nanoTime();
            this.runnable = runnable;
        }

        @Override
        public long getDelay(TimeUnit timeUnit) {
            return timeUnit.convert(this.timeout - System.nanoTime(), TimeUnit.NANOSECONDS);
        }

        @Override
        public int compareTo(Delayed delayed) {
            if (delayed == this) {
                return 0;
            }
            long d = (getDelay(TimeUnit.NANOSECONDS) - delayed.getDelay(TimeUnit.NANOSECONDS));

            return (d == 0) ? 0 : ((d < 0) ? -1 : 1);
        }

        @Override
        public String toString() {
            return "DelayVO{" +
                    (null == timeout ? "" : ",  timeout = " + timeout ) +
                    (null == runnable ? "" : ", runnable = " + runnable ) +
                    (null == delayTime ? "" : ", delayTime = " + delayTime ) +
                    '}';
        }
    }
}
