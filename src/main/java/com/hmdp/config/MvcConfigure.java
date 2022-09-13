package com.hmdp.config;


import com.hmdp.utils.FlashTokenInterceptor;
import com.hmdp.utils.LoginInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;


public class MvcConfigure implements WebMvcConfigurer {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new LoginInterceptor()).excludePathPatterns("/voucher/**", "/shop-type/**", "/shop/**", "/blog/hot", "/user/code", "/user/login").order(1);
        registry.addInterceptor(new FlashTokenInterceptor(stringRedisTemplate)).order(0);
    }
}
