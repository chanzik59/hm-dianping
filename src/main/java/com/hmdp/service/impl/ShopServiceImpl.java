package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.SystemConstants;
import jodd.util.StringUtil;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private CacheClient cacheClient;


    @Override
    public Result queryById(Long id) {
//        Shop shopCache = cacheClient.getShopCacheLogicalExpire(RedisConstants.CACHE_SHOP_KEY, id, Shop.class, 30L, TimeUnit.SECONDS, this::getById);

        Shop shopCache = cacheClient.getShopCache(RedisConstants.CACHE_SHOP_KEY, id, Shop.class, 30L, TimeUnit.SECONDS, this::getById);

        if (Objects.isNull(shopCache)) {
            return Result.fail("商铺不存在");
        }
        return Result.ok(shopCache);
    }


    @Override
    @Transactional
    public Result update(Shop shop) {
        if (shop.getId() == null) {
            return Result.fail("商铺ID不存在");
        }
        updateById(shop);
        stringRedisTemplate.delete(RedisConstants.CACHE_SHOP_KEY + shop.getId());
        return Result.ok(shop);
    }

    @Override
    public Result shopTypePage(Integer typeId, Integer current, Double x, Double y) {


        if (x == null || y == null) {
            // 根据类型分页查询
            Page<Shop> page = query()
                    .eq("type_id", typeId)
                    .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
            // 返回数据
            return Result.ok(page.getRecords());
        }

        String geoKey = RedisConstants.SHOP_GEO_KEY + typeId;
        int from = (current-1)  * SystemConstants.DEFAULT_PAGE_SIZE;
        int end = current * SystemConstants.DEFAULT_PAGE_SIZE;
        GeoResults<RedisGeoCommands.GeoLocation<String>> results = stringRedisTemplate.opsForGeo().search(geoKey, GeoReference.fromCoordinate(x, y), new Distance(5000), RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs().includeDistance().limit(end));
        if (results == null) {
            return Result.ok(Collections.emptyList());
        }
        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> content = results.getContent();
        if (content.isEmpty() || content.size() <= from) {
            return Result.ok(Collections.emptyList());
        }
        TreeMap<Long, Distance> distances = new TreeMap<>();
        List<Long> ids = content.stream().skip(from).peek(v -> distances.put(Long.valueOf(v.getContent().getName()), v.getDistance())).map(v -> Long.valueOf(v.getContent().getName())).collect(Collectors.toList());

        List<Shop> shops = query().in("id", ids).last("ORDER by FIELD(id ," + StringUtil.join(ids, ",") + ")").list();
        shops = shops.stream().map(v -> v.setDistance(distances.get(v.getId()).getValue())).collect(Collectors.toList());
        return Result.ok(shops);

    }


}
