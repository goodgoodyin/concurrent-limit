package com.concurrentlimit.base.redis;

import org.apache.commons.lang3.StringUtils;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.net.URI;
import java.util.function.Function;

public class RedisServiceFactory {

    private static final RedisServiceFactory INSTANCE = new RedisServiceFactory();

    private RedisServiceFactory() {
    }

    public static Builder newBuilder() {
        return INSTANCE.new Builder();
    }


    /**
     * <br>生成 实例
     *
     * @param url
     * @return
     * @author YellowTail
     * @since 2019-06-09
     */
    public static RedisServiceImpl getService(String url) {
        return RedisServiceFactory.newBuilder()
                .url(url)
                .maxTotal(200)
                .maxIdle(50)
                .maxWaitMillis(10000L)
                .testOnBorrow(true)
                .build(new RedisServiceImpl())
                ;
    }



    public class Builder {
        private String    url;            //redis数据库连接配置
        private Integer    maxTotal;      //最大实例数
        private Integer    maxIdle;       //最大空闲实例数
        private Long    maxWaitMillis;    //(创建实例时)最大等待时间
        private boolean    testOnBorrow;  //(创建实例时)是否验证

        public Builder() {
            maxTotal = 200;
            maxIdle = 50;
            maxWaitMillis = 10000L;
            testOnBorrow = true;
        }

        /**
         * 设置 redis数据库连接配置
         * @param url
         * @return
         */
        public Builder url(String url) {
            this.url = url;
            return this;
        }

        /**
         * 最大实例数
         * @param maxTotal
         * @return
         */
        public Builder maxTotal(int maxTotal) {
            this.maxTotal = maxTotal;
            return this;
        }

        /**
         * 最大空闲实例数
         * @param maxIdle
         * @return
         */
        public Builder maxIdle(int maxIdle) {
            this.maxIdle = maxIdle;
            return this;
        }

        /**
         * (创建实例时)最大等待时间
         * @param maxWaitMillis
         * @return
         */
        public Builder maxWaitMillis(long maxWaitMillis) {
            this.maxWaitMillis = maxWaitMillis;
            return this;
        }

        public Builder testOnBorrow(boolean testOnBorrow) {
            this.testOnBorrow = testOnBorrow;
            return this;
        }

        /**
         * 生成 RedisServiceImpl 的实例
         * 没有入参，会提供一个默认的 RedisServiceImpl 实现
         * @param impl
         * @return
         */
        public RedisServiceImpl build(RedisServiceImpl impl) {
            if (StringUtils.isBlank(url)) {
                throw new IllegalArgumentException("url is required");
            }

            JedisPool jedisPool = initJedisPool(url);
            impl.setJedisPool(jedisPool);
            return impl;
        }

        /**
         * 生成 RedisServiceImpl 的实例
         * @param func func 生成 RedisServiceImpl 的实例 的构造方法
         * @return
         */
        public RedisServiceImpl build(Function<JedisPool, RedisServiceImpl> func) {
            if (StringUtils.isBlank(url)) {
                throw new IllegalArgumentException("url is required");
            }

            JedisPool jedisPool = initJedisPool(url);

            return func.apply(jedisPool);
        }

        /**
         * 初始化
         * @param url
         * @return
         */
        private JedisPool initJedisPool(String url) {
            JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
            jedisPoolConfig.setMaxTotal(maxTotal);
            jedisPoolConfig.setMaxIdle(maxIdle);
            jedisPoolConfig.setMaxWaitMillis(maxWaitMillis);
            jedisPoolConfig.setTestOnBorrow(testOnBorrow);

            // JedisPool 就是主从模式
            // ShardedJedisPool 是分片模式，也是集群模式

            return new JedisPool(jedisPoolConfig, URI.create(url));
        }
    }
}
