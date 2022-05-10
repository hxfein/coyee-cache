package com.coyee.cache.support;

/**
 * @author hxfein
 * @className: CoyeeCacheManager
 * @description: 缓存管理接口
 * @date 2022/4/28 18:21
 * @version：1.0
 */
public interface CoyeeCacheSupport {
    /**
     * 刷新对应栏目的缓存
     * @param channels
     */
    void flushChannelKeysAndCache(String[] channels);
}
