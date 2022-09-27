package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.Blog;
import com.hmdp.entity.User;
import com.hmdp.mapper.BlogMapper;
import com.hmdp.service.IBlogService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.SystemConstants;
import com.hmdp.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;

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
        Long userId = UserHolder.getUser().getId();
        Boolean like = stringRedisTemplate.opsForSet().isMember(RedisConstants.BLOG_LIKED_KEY + blog.getId(), userId.toString());
        blog.setIsLike(BooleanUtil.isTrue(like));
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

        Boolean like = stringRedisTemplate.opsForSet().isMember(blogLikeKey, userId.toString());
        if (BooleanUtil.isTrue(like)) {
            boolean updateSuccess = update().setSql("liked =  liked - 1 ").eq("id", id).update();
            if (updateSuccess) {
                stringRedisTemplate.opsForSet().remove(blogLikeKey, userId.toString());
            }
        } else {
            boolean updateSuccess = update().setSql("liked = liked + 1 ").eq("id", id).update();
            if (updateSuccess) {
                stringRedisTemplate.opsForSet().add(blogLikeKey, userId.toString());
            }
        }
        return Result.ok();
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
