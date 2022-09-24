package com.hmdp;

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
import java.time.LocalDateTime;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
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

       /* Voucher voucher = mapper.selectById(1);
        String s = JSONUtil.toJsonStr(voucher);
        System.out.println(s);*/
        System.out.println(LocalDateTime.now());
    }

    private static final ExecutorService ex = Executors.newSingleThreadExecutor();


    public static void main(String[] args) {


        BlockingQueue<Integer> queue = new ArrayBlockingQueue<>(10);


        ex.submit(() -> {
            while (true) {
                try {
                    Integer take = queue.take();
                    System.out.println(take);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

        });


        for (int i = 0; i < 100; i++) {
            queue.add(i);
            try {
                Thread.sleep(0);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }


    }


}
