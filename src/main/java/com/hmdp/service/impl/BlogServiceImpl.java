package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.dto.ScrollResult;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Blog;
import com.hmdp.entity.Follow;
import com.hmdp.entity.User;
import com.hmdp.mapper.BlogMapper;
import com.hmdp.service.IBlogService;
import com.hmdp.service.IFollowService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.SystemConstants;
import com.hmdp.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.*;
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
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {
    @Resource
    private IUserService userService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private IFollowService followService;

    @Override
    public Result queryBlogById(Long blogId) {
        Blog blog = getById(blogId);
        if (Objects.isNull(blog)) {
            Result.fail("博客不存在");
        }
        setBlogUserInfo(blog);
        checkIsLike(blog);
        return Result.ok(blog);
    }


    /**
     * 检查此博客是否被用户点赞过
     *
     * @param blog
     */
    private void checkIsLike(Blog blog) {
        UserDTO user = UserHolder.getUser();
        if (Objects.isNull(user)) {
            return;
        }
        Long userId = user.getId();
        Double score = stringRedisTemplate.opsForZSet().score(RedisConstants.BLOG_LIKED_KEY + blog.getId(), userId.toString());
        blog.setIsLike(Objects.nonNull(score));
    }

    @Override
    public Result queryHotBlog(Integer current) {
        Page<Blog> page = query()
                .orderByDesc("liked")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        // 查询用户
        records.forEach(this::setBlogUserInfo);
        return Result.ok(records);
    }

    @Override
    public Result likeBlogById(Long id) {
        Long userId = UserHolder.getUser().getId();
        String blogLikeKey = RedisConstants.BLOG_LIKED_KEY + id;

        Double score = stringRedisTemplate.opsForZSet().score(blogLikeKey, userId.toString());
        if (Objects.nonNull(score)) {
            boolean updateSuccess = update().setSql("liked =  liked - 1 ").eq("id", id).update();
            if (updateSuccess) {
                stringRedisTemplate.opsForZSet().remove(blogLikeKey, userId.toString());
            }
        } else {
            boolean updateSuccess = update().setSql("liked = liked + 1 ").eq("id", id).update();
            if (updateSuccess) {
                stringRedisTemplate.opsForZSet().add(blogLikeKey, userId.toString(), System.currentTimeMillis());
            }
        }
        return Result.ok();
    }

    @Override
    public Result queryLikesById(Long id) {
        Set<String> userIds = stringRedisTemplate.opsForZSet().range(RedisConstants.BLOG_LIKED_KEY + id, 0, 4);
        if (CollectionUtils.isEmpty(userIds)) {
            return Result.ok(Collections.emptyList());
        }
        List<Long> userIdList = userIds.stream().map(Long::parseLong).collect(Collectors.toList());
        String order = StrUtil.join(",", userIdList);
        List<User> users = userService.query().in("id", userIdList).last("ORDER by FIELD(id ," + order + ")").list();
        List<UserDTO> collect = users.stream().map(v -> BeanUtil.copyProperties(v, UserDTO.class)).collect(Collectors.toList());
        return Result.ok(collect);
    }

    @Override
    public Result saveBlog(Blog blog) {
        // 获取登录用户
        UserDTO user = UserHolder.getUser();
        blog.setUserId(user.getId());
        // 保存探店博文
        boolean success = save(blog);
        if (!success) {
            return Result.fail("博客发布失败");
        }
        List<Follow> follows = followService.query().eq("follow_user_id", user.getId()).list();
        for (Follow follow : follows) {
            stringRedisTemplate.opsForZSet().add(RedisConstants.FEED_KEY + follow.getUserId(), blog.getId().toString(), System.currentTimeMillis());
        }
        // 返回id
        return Result.ok(blog.getId());
    }

    @Override
    public Result followBlogs(Long max, Integer offset) {

        Long userId = UserHolder.getUser().getId();

        Set<ZSetOperations.TypedTuple<String>> tuples = stringRedisTemplate.opsForZSet().reverseRangeByScoreWithScores(RedisConstants.FEED_KEY + userId, 0, max, offset, 5);
        if (tuples == null || tuples.isEmpty()) {
            return Result.ok();
        }
        offset = 1;
        max = 0L;
        LinkedList<Long> blogIds = new LinkedList<>();
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            long minTime = tuple.getScore().longValue();
            blogIds.add(Long.valueOf(tuple.getValue()));
            if (minTime == max) {
                offset++;
            } else {
                max = minTime;
                offset = 1;
            }
        }
        List<Blog> blogs = query().in("id", blogIds).last("ORDER by FIELD(id ," + StrUtil.join(",", blogIds) + ")").list();
        ScrollResult<Blog> blogScrollResult = new ScrollResult<>();
        blogScrollResult.setList(blogs);
        blogScrollResult.setOffset(offset);
        blogScrollResult.setMinTime(max);
        return Result.ok(blogScrollResult);
    }

    /**
     * 给博客设置用户信息
     *
     * @param blog
     */
    private void setBlogUserInfo(Blog blog) {
        checkIsLike(blog);
        Long userId = blog.getUserId();
        User user = userService.getById(userId);
        blog.setName(user.getNickName());
        blog.setIcon(user.getIcon());
    }
}
