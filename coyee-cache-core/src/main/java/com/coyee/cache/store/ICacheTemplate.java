package com.coyee.cache.store;

import java.io.Serializable;
import java.util.Collection;
import java.util.Set;

/**
 * @author hxfein
 * @className: ICacheTemplate
 * @description: 缓存操作模板，用于适配不同的缓存存储
 * @date 2022/4/25 11:40
 * @version：1.0
 */
public interface ICacheTemplate {
    /**
     * 存储
     * @param key
     * @param value
     * @param expires
     */
    void put(String key, Serializable value,long expires);

    /**
     * 获取缓存数据
     * @param key
     * @return
     */
    Serializable get(String key);

    /**
     * 获取频道关联的缓存KEY
     * @param channel
     * @return
     */
    Set<String> keysOfChannel(String channel);

    /**
     * 将key保存到相关的频道
     * @param key
     * @param channel
     */
    void addKeysToChannels(String channel,String key);

    /**
     * 删除缓存
     * @param channel
     */
    void deleteChannel(String channel);

    /**
     * 批量删除缓存
     * @param keys
     */
    void delete(Collection<String> keys);

}
