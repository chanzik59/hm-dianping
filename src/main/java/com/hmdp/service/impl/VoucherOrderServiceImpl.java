package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.hmdp.utils.RedisIDMaker;
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
@Slf4j
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private ISeckillVoucherService seckillVoucherService;
    @Resource
    private RedisIDMaker redisIDMaker;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private StringRedisTemplate stringRedisTemplate;


    /**
     * lua脚本
     */
    private static final DefaultRedisScript<Long> SEC_KILL_SCRIPT;


    private BlockingQueue<VoucherOrder> secKillQueue = new ArrayBlockingQueue(1024 * 1024);

    private static final ExecutorService ORDER_EXECUTOR = Executors.newSingleThreadExecutor();


    private IVoucherOrderService voucherOrderService;

    static {
        SEC_KILL_SCRIPT = new DefaultRedisScript();
        SEC_KILL_SCRIPT.setLocation(new ClassPathResource("secKill.lua"));
        SEC_KILL_SCRIPT.setResultType(Long.class);

    }

    /**
     * 开启阻塞队列消费内容
     */
    @PostConstruct
    private void init() {
        ORDER_EXECUTOR.submit(() -> {
            try {
                while (true) {
                    List<MapRecord<String, Object, Object>> mapRecords = stringRedisTemplate.opsForStream().read(Consumer.from("g1", "c1"), StreamReadOptions.empty().count(1).block(Duration.ofSeconds(2)), StreamOffset.create("stream.orders", ReadOffset.lastConsumed()));

                    if (Objects.isNull(mapRecords) || mapRecords.isEmpty()) {
                        continue;
                    }
                    MapRecord<String, Object, Object> entries = mapRecords.get(0);
                    VoucherOrder voucherOrder = BeanUtil.toBean(entries.getValue(), VoucherOrder.class);
                    RLock lock = redissonClient.getLock("redis:lock:" + voucherOrder.getUserId().toString());
                    try {
                        //使用用户id常量池对象加锁 ，事务结束解锁
                        if (!lock.tryLock()) {
                            log.error("重复下单");
                            return;
                        }

                        voucherOrderService.limitNum(voucherOrder);
                        stringRedisTemplate.opsForStream().acknowledge("stream.orders", "g1", entries.getId());
                    } catch (Exception e) {
                        while (true) {
                            mapRecords = stringRedisTemplate.opsForStream().read(Consumer.from("g1", "c1"), StreamReadOptions.empty().count(1), StreamOffset.create("stream.orders", ReadOffset.from("0")));
                            voucherOrder = BeanUtil.toBean(entries.getValue(), VoucherOrder.class);
                            if (Objects.isNull(mapRecords) || mapRecords.isEmpty()) {
                                break;
                            }
                            voucherOrderService.limitNum(voucherOrder);
                            stringRedisTemplate.opsForStream().acknowledge("stream.orders", "g1", entries.getId());
                            Thread.sleep(200);
                        }
                    } finally {
                        lock.unlock();
                    }
                }
            } catch (Exception e) {
                log.error("入库失败", e);
            }
        });

    }


    @Override
    public Result secKill(Long voucherId) {
        UserDTO user = UserHolder.getUser();
        long orderId = redisIDMaker.nextId("voucher:order");
        Long result = stringRedisTemplate.execute(SEC_KILL_SCRIPT, Collections.emptyList(), voucherId.toString(), user.getId().toString(), String.valueOf(orderId));
        voucherOrderService = (IVoucherOrderService) AopContext.currentProxy();
        if (result != 0) {
            return Result.fail(result == 1 ? "库存不足" : "不能重复下单");

        }

        return Result.ok(orderId);
    }

/*
    @Override
    public Result secKill(Long voucherId) {
        SeckillVoucher seckillVoucher = seckillVoucherService.getById(voucherId);
        if (seckillVoucher.getBeginTime().isAfter(LocalDateTime.now()) || seckillVoucher.getEndTime().isBefore(LocalDateTime.now())) {
            return Result.fail("不在秒杀时间内");
        }
        if (seckillVoucher.getStock() < 1) {
            return Result.fail("库存不足");
        }
        UserDTO user = UserHolder.getUser();
        RLock lock = redissonClient.getLock("redis:lock:" + user.getId().toString());
        //使用用户id常量池对象加锁 ，事务结束解锁
        if (!lock.tryLock()) {
            return Result.fail("重复下单");
        }
        try {
            //获取到代理对象调用事务
            IVoucherOrderService voucherOrderService = (IVoucherOrderService) AopContext.currentProxy();
            return voucherOrderService.limitNum(voucherId);
        } catch (Exception e) {
            log.error("中断异常", e);
        } finally {
            lock.unlock();
        }
        return Result.fail("下单失败");
    }*/


    /**
     * 限制用户下单数量
     *
     * @param voucherOrder
     * @return
     */
    @Transactional
    @Override
    public void limitNum(VoucherOrder voucherOrder) {
        Integer count = query().eq("voucher_id", voucherOrder.getVoucherId()).eq("user_id", voucherOrder.getUserId()).count();
        if (count > 0) {
            log.error("重复下单");

            return;
        }
        boolean success = seckillVoucherService.update().setSql("stock =  stock -1").eq("voucher_id", voucherOrder.getVoucherId()).gt("stock", 0).update();
        if (!success) {
            log.error("下单失败");
            return;
        }
        save(voucherOrder);
    }
}
