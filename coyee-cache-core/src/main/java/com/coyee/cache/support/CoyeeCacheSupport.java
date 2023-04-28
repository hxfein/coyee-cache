package com.coyee.cache.support;

import java.io.Serializable;
import java.util.Map;

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
     *
     * @param channels
     */
    void flushChannelKeysAndCache(String[] channels);
    /**
     * 保存数据到缓存并建立关联
     *
     * @param key
     * @param channels
     * @param raw
     * @param expires
     */
    void putChannelAndCache(String key, String[] channels, Serializable raw, long expires);

    /**
     * 保存数据到缓存并建立关联
     * @param key
     * @param channels
     * @param raw
     */
    void putChannelAndCache(String key, String[] channels, Serializable raw);

    /**
     * 从缓存中取值
     * @param key
     * @return
     */
    Serializable get(String key);
    /**
     * 获取统计信息
     *
     * @return
     */
    Map<String, Object> getStats();
}
