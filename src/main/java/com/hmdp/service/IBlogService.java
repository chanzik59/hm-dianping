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
}
