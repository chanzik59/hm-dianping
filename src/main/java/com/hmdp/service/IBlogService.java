package com.hmdp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.dto.Result;
import com.hmdp.entity.Blog;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IBlogService extends IService<Blog> {

    /**
     * 查询博客
     *
     * @param blogId
     * @return
     */
    Result queryBlogById(Long blogId);

    /**
     * 查询热点博客
     *
     * @param current
     * @return
     */
    Result queryHotBlog(Integer current);

    /**
     * 博客点赞功能
     *
     * @param id
     * @return
     */
    Result likeBlogById(Long id);

    /**
     * 查询最先点赞的五位用户
     *
     * @param id
     * @return
     */
    Result queryLikesById(Long id);

    /**
     * 保存博客并且推送关注收件箱
     *
     * @param blog
     * @return
     */
    Result saveBlog(Blog blog);

    /**
     * 滚动查询关注推送
     *
     * @param minTime
     * @param offset
     * @return
     */
    Result followBlogs(Long minTime, Integer offset);
}
