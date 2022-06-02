package com.coyee.cache.template;

import com.coyee.cache.bean.Data;
import com.coyee.cache.exception.CacheException;
import com.coyee.cache.store.ICacheTemplate;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.data.redis.core.RedisTemplate;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author hxfein
 * @className: RedisCacheTemplate
 * @description: com.coyee.cache.template
 * @date 2022/4/25 16:04
 * @version：1.0
 */
public class RedisCacheTemplate implements ICacheTemplate {
    private static Log log = LogFactory.getLog(RedisCacheTemplate.class);
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
    public Serializable get(String key) {
        String storeKey = this.createKey(key);
        return redisTemplate.opsForValue().get(storeKey);
    }

    @Override
    public void clearChannelAndCache(String channel) {
        String linkKey = this.createLinkKey(channel);
        Set<Serializable> linkChannels = redisTemplate.opsForSet().members(linkKey);
        for (Serializable linkChannel : linkChannels) {
            clearLinkChannelAndCache((String) linkChannel);
        }
        this.clearLinkChannelAndCache(channel);
    }

    /**
     * 清除一个栏目的缓存和关联关系
     *
     * @param channel
     */
    private void clearLinkChannelAndCache(String channel) {
        String channelKey = this.createChannelKey(channel);
        Set<Serializable> keys = redisTemplate.opsForSet().members(channelKey);
        Set<String> keySet = keys.stream().map((key) -> this.createKey((String)key)).collect(Collectors.toSet());
        this.deleteByKeys(keySet);//删除栏目相关的数据
        this.deleteByKey(channelKey);//删除栏目
        String linkKey = this.createLinkKey(channel);
        this.deleteByKey(linkKey);//删除栏目关联
    }

    @Override
    public void putChannelAndCache(String key, String[] channels, Serializable raw, long expires) {
        String storeKey = this.createKey(key);
        redisTemplate.opsForValue().set(storeKey, new Data(raw), expires, TimeUnit.MILLISECONDS);//保存实际缓存数据
        for (String channel : channels) {
            String linkKey = this.createLinkKey(channel);
            redisTemplate.opsForSet().add(linkKey, channels);//建立栏目与其它栏目的关联关系
            String channelKey = this.createChannelKey(channel);
            redisTemplate.opsForSet().add(channelKey, key);//建立栏目与缓存数据的关系，此处保留原始的key,不加前缀
        }
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
     * 创建栏目存储的位置KEY
     *
     * @param channel
     * @return
     */
    private String createChannelKey(String channel) {
        return namespace + ":channel:" + channel;
    }

    /**
     * 创建关联关系存储键
     *
     * @param channel
     * @return
     */
    private String createLinkKey(String channel) {
        return namespace + ":links:" + channel;
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
