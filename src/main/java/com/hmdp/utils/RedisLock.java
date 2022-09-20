package com.hmdp.utils;


import cn.hutool.core.lang.UUID;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * 分布式锁解决分布式锁问题  尚存问题：判断属于自己锁和删除锁不是原子性的操作，误删锁依旧存在，并且无法防止 堵塞时间过长，ttl失效 个人超卖限单问题
 */
public class RedisLock implements ILock {

    private String name;
    private StringRedisTemplate redisTemplate;
    private static final String KEY_PREFIX = "redis:lock:";

    private String lock_prefix = UUID.randomUUID().toString(true);

    /**
     * lua脚本
     */
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;

    static {
        UNLOCK_SCRIPT = new DefaultRedisScript();
        UNLOCK_SCRIPT.setLocation(new ClassPathResource("unlock.lua"));
        UNLOCK_SCRIPT.setResultType(Long.class);
    }

    public RedisLock(String name, StringRedisTemplate redisTemplate) {
        this.name = name;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean tryLock(Long timeout) {
        return redisTemplate.opsForValue().setIfAbsent(KEY_PREFIX + name, lock_prefix + Thread.currentThread().getId(), timeout, TimeUnit.SECONDS);
    }

    @Override
    public void unlock() {
        redisTemplate.execute(UNLOCK_SCRIPT, Collections.singletonList(KEY_PREFIX + name), lock_prefix + Thread.currentThread().getId());
    }
}
