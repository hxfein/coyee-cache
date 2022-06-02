package com.coyee.cache.store;

import com.coyee.cache.annotation.Cache;

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
     * 获取缓存数据
     * @param key
     * @return
     */
    Serializable get(String key);

    /**
     * 删除栏目缓存数据和相关关联数据
     * @param channel
     */
    void clearChannelAndCache(String channel);

    /**
     * 保存数据到缓存并建立关联
     * @param key
     * @param channels
     * @param raw
     * @param expires
     */
    void putChannelAndCache(String key, String[] channels, Serializable raw,long expires);

}
