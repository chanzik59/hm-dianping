package com.hmdp.utils;

public interface ILock {
    /**
     * 获取分布式锁
     *
     * @param timeout
     * @return
     */
    boolean tryLock(Long timeout);

    /**
     * 解除分布式锁
     */
    void unlock();
}
