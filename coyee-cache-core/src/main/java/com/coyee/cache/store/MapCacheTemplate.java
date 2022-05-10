package com.coyee.cache.store;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author hxfein
 * @className: MapCacheTemplate
 * @description: 以map实现缓存，主要用于调试
 * @date 2022/4/27 19:24
 * @version：1.0
 */
public class MapCacheTemplate implements ICacheTemplate {
    private Map<String, Serializable> cacheMap = new HashMap<>();
    private Map<String, Set<Serializable>> channelKeysMap = new HashMap<>();

    @Override
    public void put(String key, Serializable value, long expires) {
        cacheMap.put(key, value);
    }

    @Override
    public Serializable get(String key) {
        return cacheMap.get(key);
    }

    @Override
    public Set<String> keysOfChannel(String channel) {
        Set<Serializable> keys = channelKeysMap.get(channel);
        if(keys==null){
            return Collections.emptySet();
        }
        return keys.stream().map((key)-> (String)key).collect(Collectors.toSet());
    }

    @Override
    public void addKeysToChannels(String channel, String key) {
        Set<Serializable> keys = channelKeysMap.get(channel);
        if (keys == null) {
            keys = new HashSet<>();
        }
        keys.add(key);
        channelKeysMap.put(channel, keys);
    }

    @Override
    public void deleteChannel(String channel) {
        channelKeysMap.remove(channel);
    }

    @Override
    public void delete(Collection<String> keys) {
        if (keys == null) {
            return;
        }
        for (Serializable key : keys) {
            cacheMap.remove(key);
        }
    }
}
