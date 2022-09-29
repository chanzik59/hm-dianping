package com.hmdp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.dto.Result;
import com.hmdp.entity.Follow;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IFollowService extends IService<Follow> {

    /**
     * 关注用户或者取消关注用户
     *
     * @param id     被操作对象的用户id
     * @param follow true 关注 false 取消关注
     * @return
     */
    Result follow(Long id, Boolean follow);

    /**
     * 查询是否关注该用户
     *
     * @param id
     * @return
     */
    Result isFollow(Long id);
}
