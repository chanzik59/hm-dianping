package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Follow;
import com.hmdp.mapper.FollowMapper;
import com.hmdp.service.IFollowService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {


    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private IUserService userService;


    @Override
    public Result follow(Long id, Boolean follow) {
        Long userId = UserHolder.getUser().getId();
        String redisFollowKey = RedisConstants.REDIS_FOLLOW + userId;
        if (follow) {
            Follow followDo = new Follow();
            followDo.setFollowUserId(id);
            followDo.setUserId(userId);
            boolean save = save(followDo);
            Optional.of(save).filter(v -> v).ifPresent(v -> stringRedisTemplate.opsForSet().add(redisFollowKey, id.toString()));
        } else {
            boolean remove = remove(new QueryWrapper<Follow>().eq("user_id", userId).eq("follow_user_id", id));
            Optional.of(remove).filter(v -> v).ifPresent(v -> stringRedisTemplate.opsForSet().remove(redisFollowKey, id.toString()));
        }
        return Result.ok();
    }

    @Override
    public Result isFollow(Long id) {
        Long userId = UserHolder.getUser().getId();
        Integer count = query().eq("user_id", userId).eq("follow_user_id", id).count();
        return Result.ok(count > 0);
    }

    @Override
    public Result followCommons(Long id) {
        Long userId = UserHolder.getUser().getId();
        Set<String> commons = stringRedisTemplate.opsForSet().intersect(RedisConstants.REDIS_FOLLOW + userId, RedisConstants.REDIS_FOLLOW + id);
        if (CollectionUtils.isEmpty(commons)) {
            return Result.ok();
        }
        List<Long> ids = commons.stream().map(Long::valueOf).collect(Collectors.toList());
        List<UserDTO> users = userService.listByIds(ids).stream().map(v -> BeanUtil.copyProperties(v, UserDTO.class)).collect(Collectors.toList());
        return Result.ok(users);
    }
}
