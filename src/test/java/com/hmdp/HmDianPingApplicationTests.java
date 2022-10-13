package com.hmdp;

import com.hmdp.entity.Shop;
import com.hmdp.mapper.VoucherMapper;
import com.hmdp.service.impl.ShopServiceImpl;
import com.hmdp.service.impl.VoucherServiceImpl;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RedisIDMaker;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

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


    @Value("${spring.redis.host}")
    private String redisHost;

    private ExecutorService executorService = Executors.newFixedThreadPool(500);


    @Test
    void test() throws InterruptedException {

        System.out.println(redisHost);
    }

    /**
     * 导入数据库店铺地理信息到redis
     */
    @Test
    void loadShopGeo() {
        Map<Long, List<Shop>> shopTypes = shopService.list().stream().collect(Collectors.groupingBy(Shop::getTypeId));
        Map<String, Map<String, Point>> typeGeos = shopTypes.entrySet().stream().collect(Collectors.toMap(v -> RedisConstants.SHOP_GEO_KEY + v.getKey(), v ->
                v.getValue().stream().collect(Collectors.toMap(v1 -> String.valueOf(v1.getId()), v2 -> new Point(v2.getX(), v2.getY())))));
        typeGeos.entrySet().stream().forEach(v -> stringRedisTemplate.opsForGeo().add(v.getKey(), v.getValue()));

    }


}
