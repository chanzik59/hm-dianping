package com.hmdp.service.impl;

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
import org.redisson.api.RedissonClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Collections;

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

    static {
        SEC_KILL_SCRIPT = new DefaultRedisScript();
        SEC_KILL_SCRIPT.setLocation(new ClassPathResource("secKill.lua"));
        SEC_KILL_SCRIPT.setResultType(Long.class);

    }

    @Override
    public Result secKill(Long voucherId) {
        UserDTO user = UserHolder.getUser();
        Long result = stringRedisTemplate.execute(SEC_KILL_SCRIPT, Collections.emptyList(), voucherId.toString(), user.getId().toString());

        if (result != 0) {
            return Result.fail(result == 1 ? "库存不足" : "不能重复下单");

        }
        long order = redisIDMaker.nextId("order");
        return Result.ok(order);
    }



/*    @Override
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
     * @param voucherId
     * @return
     */
    @Transactional
    @Override
    public Result limitNum(Long voucherId) {
        UserDTO user = UserHolder.getUser();
        Integer count = query().eq("voucher_id", voucherId).eq("user_id", user.getId()).count();
        if (count > 0) {
            return Result.fail("该用户已超过限定单数");
        }
        boolean success = seckillVoucherService.update().setSql("stock =  stock -1").eq("voucher_id", voucherId).gt("stock", 0).update();
        if (!success) {
            return Result.fail("抢购失败！！");
        }
        VoucherOrder voucherOrder = new VoucherOrder();
        long orderId = redisIDMaker.nextId("voucher:order");
        voucherOrder.setId(orderId);
        voucherOrder.setVoucherId(voucherId);
        voucherOrder.setUserId(UserHolder.getUser().getId());
        save(voucherOrder);
        return Result.ok(orderId);
    }
}
