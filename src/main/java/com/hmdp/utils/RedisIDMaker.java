package com.hmdp.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Component
public class RedisIDMaker {
    /**
     * 基准时间
     */
    private static final Long BEGIN_TIME = 1663365600L;


    @Autowired
    private StringRedisTemplate stringRedisTemplate;


    /**
     * 生成唯一ID  秒时间+自增redisID
     *
     * @param keyPrefix
     * @return
     */
    public long nextId(String keyPrefix) {
        long timestamp = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) - BEGIN_TIME;
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        Long increment = stringRedisTemplate.opsForValue().increment("icr:" + keyPrefix + ":" + date);
        return timestamp << 32 | increment;
    }
}
