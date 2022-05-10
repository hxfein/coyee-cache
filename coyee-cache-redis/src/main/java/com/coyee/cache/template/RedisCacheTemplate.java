package com.coyee.cache.template;

import com.coyee.cache.store.ICacheTemplate;
import org.apache.commons.lang.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;

import javax.annotation.Resource;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author hxfein
 * @className: RedisCacheTemplate
 * @description: com.coyee.cache.template
 * @date 2022/4/25 16:04
 * @version：1.0
 */
public class RedisCacheTemplate implements ICacheTemplate {
    @Resource
    private RedisTemplate<String, Serializable> redisTemplate;
    /**
     * 存储的目录名
     */
    private static final String DEFAULT_NAMESPACE = "coyee_cache";
    private String namespace = DEFAULT_NAMESPACE;

    @Override
    public void put(String key, Serializable value, long expires) {
        String storeKey = this.createKey(key);
        redisTemplate.opsForValue().set(storeKey, value);
    }

    @Override
    public Serializable get(String key) {
        String storeKey = this.createKey(key);
        return redisTemplate.opsForValue().get(storeKey);
    }

    @Override
    public Set<String> keysOfChannel(String channel) {
        String channelKey = this.createChannelKey(channel);
        Set<Serializable> keys = redisTemplate.opsForSet().members(channelKey);
        if (keys == null) {
            return Collections.emptySet();
        }
        return keys.stream().map((key) -> (String) key).collect(Collectors.toSet());
    }

    @Override
    public void addKeysToChannels(String channel, String key) {
        String channelKey = this.createChannelKey(channel);
        redisTemplate.opsForSet().add(channelKey, key);//此处保留原始的key,不加前缀
    }

    @Override
    public void deleteChannel(String channel) {
        String channelKey = this.createChannelKey(channel);
        redisTemplate.delete(channelKey);
    }

    @Override
    public void delete(Collection<String> keys) {
        List<String> storeKeys = this.createKeys(keys);
        redisTemplate.delete(storeKeys);
    }







    /**
     * 创建存储的位置KEY
     *
     * @param key
     * @return
     */
    private String createKey(String key) {
        key= StringUtils.replaceChars(key,':','=');
        return namespace + ":data:" + key;
    }

    /**
     * 创建存储的位置KEY
     *
     * @param keys
     * @return
     */
    private List<String> createKeys(Collection<String> keys) {
        if (keys == null) {
            return Collections.emptyList();
        }
        return keys.stream().map(this::createKey).collect(Collectors.toList());
    }

    /**
     * 创建栏目存储的位置KEY
     *
     * @param channelKey
     * @return
     */
    private String createChannelKey(String channelKey) {
        return namespace+ ":channel:" + channelKey;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }
}
