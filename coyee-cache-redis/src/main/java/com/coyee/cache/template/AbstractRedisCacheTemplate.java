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

/**
 * @author hxfein
 * @className: AbstractRedisCacheTemplate
 * @description: redis缓存管理实现的抽象类
 * @date 2022/6/22 9:36
 * @version：1.0
 */
public abstract class AbstractRedisCacheTemplate implements ICacheTemplate {
    @Resource
    protected RedisTemplate<Serializable, Serializable> redisTemplate;
    /**
     * 存储的目录名
     */
    protected static final String DEFAULT_NAMESPACE = "coyee_cache";
    protected String namespace = DEFAULT_NAMESPACE;
    /**
     * 是否启用兼容模式
     */
    protected boolean compatibilityMode = false;
    /**
     * 批量删除的方法
     */
    protected Method deleteBatchMethod = null;
    /**
     * 删除单个的方法
     */
    protected Method deleteSingleMethod = null;

    /**
     * 是否启用严格的一致性保护
     */
    protected boolean strict = true;

    /**
     * 清除channel下无效的key
     *
     * @param channel
     */
    public abstract long clearInvalidChannelKeys(String channel);

    /**
     * 查询当前数据库ID
     *
     * @return
     */
    public abstract long getDbIndex();

    /**
     * 处理key过期事件
     *
     * @param message
     * @return 返回true表示已处理, 返回false表示交由下一个处理器处理
     */
    public abstract boolean handleKeyExpiredMessage(String message);


    @PostConstruct
    protected void init() {
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
    protected void deleteByKey(String key) {
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
    protected void deleteByKeys(Collection<Serializable> keys) {
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

    /**
     * 创建存储的位置KEY
     *
     * @param key
     * @return
     */
    protected String createKey(String key) {
        key = StringUtils.replaceChars(key, ':', '=');
        return namespace + ":data:" + key;
    }

    /**
     * 创建栏目存储的位置KEY
     *
     * @param channel
     * @return
     */
    protected String createChannelKey(String channel) {
        return namespace + ":channel:" + channel;
    }

    /**
     * 清除栏目数据定时器key
     *
     * @param channel
     * @return
     */
    protected String createTimerKey(String channel) {
        return namespace + ":timer:" + channel;
    }

    /**
     * 获取定时器KEY的前缀
     *
     * @return
     */
    public String getTimerKeyPrefix() {
        return namespace + ":timer:";
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

    public RedisTemplate<Serializable, Serializable> getRedisTemplate() {
        return redisTemplate;
    }

    public void setRedisTemplate(RedisTemplate<Serializable, Serializable> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean isStrict() {
        return strict;
    }

    public void setStrict(boolean strict) {
        this.strict = strict;
    }
}
