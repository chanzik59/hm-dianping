package com.hmdp.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 配置redisson
 */
@Configuration
public class RedissonConfig {

    /**
     * redis 通讯地址
     */
    @Value("${spring.redis.host}")
    private String redisHost;
    /**
     * redis 端口
     */
    @Value("${spring.redis.port}")
    private String redisPort;
    /**
     * 登录密码
     */
    @Value("${spring.redis.password}")
    private String redisPassword;

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();

        config.useSingleServer().setAddress(String.format("redis://%s:%s", redisHost, redisPort)).setPassword(redisPassword);
        return Redisson.create(config);
    }
}
