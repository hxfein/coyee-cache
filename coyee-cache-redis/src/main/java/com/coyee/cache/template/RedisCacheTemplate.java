package com.coyee.cache.template;

import com.coyee.cache.exception.CacheException;
import com.coyee.cache.store.ICacheTemplate;
import org.apache.commons.lang.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
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
    /**
     * 是否启用兼容模式
     */
    private boolean compatibilityMode = false;


    /**
     * 批量删除的方法
     */
    private Method deleteBatchMethod = null;
    /**
     * 删除单个的方法
     */
    private Method deleteSingleMethod = null;

    @PostConstruct
    private void init() {
        try {
            Class<?> clazz = redisTemplate.getClass();
            deleteBatchMethod = clazz.getMethod("delete", Collection.class);
            deleteSingleMethod = clazz.getMethod("delete", Object.class);
        } catch (Exception e) {
            throw new CacheException("初始化redisTemplate出错", e);
        }
    }

    /**
     * 删除key
     * 通过反射方式兼容低版本redis客户端与高版本删除方法的差异
     *
     * @param key
     */
    private void deleteByKey(String key) {
        if (compatibilityMode == true) {
            try {
                deleteSingleMethod.invoke(redisTemplate, key);
            } catch (Exception er) {
                throw new CacheException("通过兼容模式删除删除失败", er);
            }
        } else {
            redisTemplate.delete(key);
        }
    }

    /**
     * 删除keys
     * 通过反射方式兼容低版本redis客户端与高版本删除方法的差异
     *
     * @param keys
     */
    private void deleteByKeys(Collection<String> keys) {
        if (compatibilityMode == true) {
            try {
                deleteBatchMethod.invoke(redisTemplate, keys);
            } catch (Exception er) {
                throw new CacheException("通过兼容模式删除删除失败", er);
            }
        } else {
            redisTemplate.delete(keys);
        }
    }

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
        this.deleteByKey(channelKey);
    }

    @Override
    public void delete(Collection<String> keys) {
        List<String> storeKeys = this.createKeys(keys);
        this.deleteByKeys(storeKeys);
    }


    /**
     * 创建存储的位置KEY
     *
     * @param key
     * @return
     */
    private String createKey(String key) {
        key = StringUtils.replaceChars(key, ':', '=');
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
        return namespace + ":channel:" + channelKey;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public boolean isCompatibilityMode() {
        return compatibilityMode;
    }

    public void setCompatibilityMode(boolean compatibilityMode) {
        this.compatibilityMode = compatibilityMode;
    }
}
