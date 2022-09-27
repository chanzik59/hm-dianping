package com.hmdp.utils;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Component
@Slf4j
public class CacheClient {
    private StringRedisTemplate stringRedisTemplate;
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    public CacheClient(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }


    /**
     * 将对象设置入自动过期
     *
     * @param key
     * @param value
     * @param time
     * @param timeUnit
     */
    public void setCache(String key, Object value, Long time, TimeUnit timeUnit) {
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value), time, timeUnit);
    }


    /**
     * 设置逻辑过期缓存
     *
     * @param key
     * @param value
     * @param time
     * @param timeUnit
     */
    public void setLogicalCache(String key, Object value, Long time, TimeUnit timeUnit) {
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(timeUnit.toSeconds(time)));
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
    }

    /**
     * 缓存空对象 解决缓存穿透
     *
     * @param prefix
     * @param id
     * @param tClass
     * @param function
     * @param <T>
     * @param <R>
     * @return
     */
    public <T, R> R getShopCache(String prefix, T id, Class<R> tClass, Long time, TimeUnit timeUnit, Function<T, R> function) {
        String dataJson = stringRedisTemplate.opsForValue().get(prefix + id);
        if (StrUtil.isNotBlank(dataJson)) {
            return JSONUtil.toBean(dataJson, tClass);
        }
        if (dataJson != null) {
            return null;
        }
        R result = function.apply(id);
        if (Objects.isNull(result)) {
            //缓存空对象
            stringRedisTemplate.opsForValue().set(RedisConstants.CACHE_SHOP_KEY + id, "", time, timeUnit);
            return null;
        }
        stringRedisTemplate.opsForValue().set(RedisConstants.CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(result), time, timeUnit);
        return result;
    }


    /**
     * 缓存击穿解决 逻辑过期
     *
     * @param prefix
     * @param id
     * @param rClass
     * @param time
     * @param timeUnit
     * @param function
     * @param <R>
     * @param <T>
     * @return
     */
    public <R, T> R getShopCacheLogicalExpire(String prefix, T id, Class<R> rClass, Long time, TimeUnit timeUnit, Function<T, R> function) {
        String dataJson = stringRedisTemplate.opsForValue().get(prefix + id);
        if (StrUtil.isBlank(dataJson)) {
            return null;
        }
        RedisData redisData = JSONUtil.toBean(dataJson, RedisData.class);
        R result = JSONUtil.toBean((JSONObject) redisData.getData(), rClass);
        LocalDateTime expireTime = redisData.getExpireTime();
        if (expireTime.isAfter(LocalDateTime.now())) {
            return result;
        }
        String lockKey = RedisConstants.LOCK_SHOP_KEY + id;
        if (tryLock(lockKey)) {
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                try {
                    R result1 = function.apply(id);
                    Thread.sleep(300L);
                    setLogicalCache(prefix + id, result1, time, timeUnit);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    unLock(lockKey);
                }
            });
        }
        return result;
    }


    /**
     * 获取互斥锁
     *
     * @param key
     * @return
     */
    private boolean tryLock(String key) {
        Boolean aBoolean = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(aBoolean);
    }


    /**
     * 删除锁
     *
     * @param key
     * @return
     */
    private boolean unLock(String key) {
        Boolean delete = stringRedisTemplate.delete(key);
        return BooleanUtil.isTrue(delete);
    }


}
