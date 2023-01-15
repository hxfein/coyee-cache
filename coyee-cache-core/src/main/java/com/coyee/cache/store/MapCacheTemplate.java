package com.coyee.cache.store;

import com.coyee.cache.bean.Data;

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
    private Map<String, Set<String>> linkChannelMap = new HashMap<>();

    @Override
    public Serializable get(String key) {
        return cacheMap.get(key);
    }


    @Override
    public void clearChannelAndCache(String channel) {
        Set<String> linkSet = linkChannelMap.get(channel);
        if (linkSet == null) {
            return;
        }
        for (String linkChannel : linkSet) {
            //删除缓存数据
            Set<Serializable> keySet = channelKeysMap.get(linkChannel);
            for (Serializable key : keySet) {
                cacheMap.remove(key);
            }
            //删除栏目与数据的关联
            channelKeysMap.remove(linkChannel);
            //删除栏目与其它栏目的关联
            linkChannelMap.remove(linkChannel);
        }
    }

    @Override
    public void putChannelAndCache(String key, String[] channels, Serializable raw, long expires) {
        //保存缓存数据
        cacheMap.put(key, new Data(raw));
        for (String channel : channels) {
            //保存栏目与缓存数据的关联关系
            Set<Serializable> keySet = channelKeysMap.get(channel);
            if (keySet == null) {
                keySet = new HashSet<>();
            }
            keySet.add(key);
            channelKeysMap.put(channel, keySet);

            //保存栏目与其它栏目的关联关系
            Set<String> linkSet = linkChannelMap.get(channel);
            if (linkSet == null) {
                linkSet = new HashSet<>();
            }
            linkSet.addAll(Arrays.asList(channels));
            linkChannelMap.put(channel, linkSet);
        }
    }
}
