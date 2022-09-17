package com.hmdp;

import cn.hutool.json.JSONUtil;
import com.hmdp.entity.Shop;
import com.hmdp.entity.Voucher;
import com.hmdp.mapper.VoucherMapper;
import com.hmdp.service.impl.ShopServiceImpl;
import com.hmdp.service.impl.VoucherServiceImpl;
import com.hmdp.utils.RedisIDMaker;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootTest
@Slf4j
@RunWith(JUnit4.class)
class HmDianPingApplicationTests {

    @Resource
    ShopServiceImpl shopService;

    @Resource
    RedisIDMaker idMaker;

    @Resource
    VoucherServiceImpl voucherService;

    @Resource
    VoucherMapper mapper;

    @Resource
    StringRedisTemplate stringRedisTemplate;

    private ExecutorService executorService = Executors.newFixedThreadPool(500);


    @Test
    void test() throws InterruptedException {

        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        System.out.println(LocalDateTime.now());
    }


}
