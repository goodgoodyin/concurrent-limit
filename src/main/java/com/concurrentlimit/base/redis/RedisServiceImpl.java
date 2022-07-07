package com.concurrentlimit.base.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisException;

import java.util.List;

public class RedisServiceImpl {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisServiceImpl.class);

    protected JedisPool jedisPool;


    protected void setJedisPool(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    /**
     * 得到  ShardedJedisPool 对象
     * @return
     */
    protected JedisPool getJedisPool() {
        return jedisPool;
    }

    public Object eval(String script, List<String> keys, List<String> args) {

        // 1. 计算 lua 脚本的 sha1
        String luaScript = cleanScript(script);
        String sha1 = HashTools.sha1(luaScript);

        // 2. 使用 sha1 直接使用脚本
        Object evalsha = null;
        try {
            evalsha = evalsha(sha1, keys, args);
        } catch (JedisException e) {

            // 3. 捕获脚本没有加载的异常，进行加载，再次执行

            String message = e.getMessage();
            if(message.startsWith("NOSCRIPT")) {
                // 提示没有脚本
                // 1. 加载
                scriptLoad(luaScript);

                // 2. 再次执行
                evalsha = evalsha(sha1, keys, args);
            } else {
                LOGGER.error("eval sha occur errors, ", e);
            }
        }

        return evalsha;
    }


    /**
     * 加载 lua 脚本
     *
     * @param script
     * @return 脚本的 sha1
     */
    public String scriptLoad(String script) {
        try(Jedis resource = getJedisPool().getResource()) {
            return resource.scriptLoad(script);
        } catch (Exception e) {
            LOGGER.error("scriptLoad error, ", e);
        }
        return null;
    }

    public Object evalsha(String sha1, List<String> keys, List<String> args) throws JedisException {
        try(Jedis resource = getJedisPool().getResource()) {

            return resource.evalsha(sha1, keys, args);
        } catch(JedisException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("evalsha error, ", e);
        }

        return null;
    }

    /**
     * lua 换行符替换为空格
     * 注释删除
     * @param lua
     * @return
     */
    private String cleanScript(String lua) {
        StringBuilder sb = new StringBuilder(256);

        String[] split = lua.split("\\n");

        for (String str : split) {
            String trim = str.trim();

            if (trim.isEmpty()) {
                continue;
            }

            if (trim.startsWith("--")) {
                //注释跳过
                continue;
            }

            sb.append(trim).append(" ");
        }

        return sb.toString();
    }
}
