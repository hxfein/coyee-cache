package com.coyee.cache.template;

import com.coyee.cache.bean.Data;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.scripting.support.StaticScriptSource;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * @author hxfein
 * @className: DefaultRedisCacheTemplate
 * @description: putChannelAndCache方法用于将数据保存在redis中并建立缓存与栏目的关系。
 * clearChannelAndCache方法用于清除缓存和缓存相关的关系。
 * @date 2022/4/25 16:04
 * @version：1.0
 */
public class DefaultRedisCacheTemplate extends AbstractRedisCacheTemplate {
    private static Log log = LogFactory.getLog(DefaultRedisCacheTemplate.class);
    //清除缓存的脚本对象
    private static DefaultRedisScript<Long> clearScript = new DefaultRedisScript<>();
    private static DefaultRedisScript<Serializable> putScript = new DefaultRedisScript<>();
    private static DefaultRedisScript<Long> clearInvalidKeysScript = new DefaultRedisScript<>();
    private static DefaultRedisScript<Long> getDbIndexScript = new DefaultRedisScript<>();

    static {
        clearScript.setResultType(Long.class);
        clearScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("clearChannelAndCache.lua")));

        clearInvalidKeysScript.setResultType(Long.class);
        clearInvalidKeysScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("clearInvalidChannelKeys.lua")));

        getDbIndexScript.setResultType(Long.class);
        getDbIndexScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("getDbIndex.lua")));

        StaticScriptSource scriptSource = new StaticScriptSource("redis.call('sadd',KEYS[1],KEYS[2]);");
        putScript.setScriptSource(scriptSource);
        putScript.setResultType(Serializable.class);
    }


    /**
     * 当前操作的数据库
     */
    private long dbIndex = -1;
    /**
     * 清除无效KEY间隔
     */
    private long clearInvalidKeysInterval = 1000 * 60 * 10;//每十分钟清除一次无效key

    private RedisConnectionFactory redisConnectionFactory;

    public DefaultRedisCacheTemplate(RedisConnectionFactory redisConnectionFactory, RedisTemplate<Serializable, Serializable> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.redisConnectionFactory = redisConnectionFactory;
    }

    @Override
    public Serializable get(String key) {
        String storeKey = this.createKey(key);
        return redisTemplate.opsForValue().get(storeKey);
    }

    @Override
    public void clearChannelAndCache(String channel) {
        List<Serializable> keys = new ArrayList<>(2);
        keys.add(namespace);
        keys.add(channel);
        redisTemplate.execute(clearScript, keys);
    }

    @Override
    public void putChannelAndCache(String key, String[] channels, Serializable raw, long expires) {
        String storeKey = this.createKey(key);
        redisTemplate.opsForValue().set(storeKey, new Data(raw), expires, TimeUnit.MILLISECONDS);//保存实际缓存数据
        for (String channel : channels) {
            String channelKey = this.createChannelKey(channel);
            this.putRelationToSet(channelKey, key);//不存储路径，减少存储消耗
            String timerKey = this.createTimerKey(channel);
            //存放定时器key,当该key过期时触发定时器清除无效KEY,每天一次
            this.setIfAbsent(timerKey,System.currentTimeMillis(),this.clearInvalidKeysInterval,TimeUnit.MILLISECONDS);
        }
    }

    /**
     * setifabsent实现，兼容旧版本API
     * @param key
     * @param value
     * @param expires
     * @param timeUnit
     */
    private void setIfAbsent(String key, Serializable value,long expires, TimeUnit timeUnit){
        redisTemplate.opsForValue().setIfAbsent(key,value);
        redisTemplate.expire(key, expires,timeUnit);
    }

    /**
     * 向set对象添加值
     *
     * @param key
     * @param value
     */
    private void putRelationToSet(String key, String value) {
        List<Serializable> keys = new ArrayList<>(2);
        keys.add(key);
        keys.add(value);
        redisTemplate.execute(putScript, keys);
    }

    @Override
    public long clearInvalidChannelKeys(String channel) {
        String timerKey = this.createTimerKey(channel);
        String channelKey = this.createChannelKey(channel);
        List<Serializable> keys = new ArrayList<>(1);
        keys.add(namespace);
        keys.add(channel);
        Long removeCount = redisTemplate.execute(clearInvalidKeysScript, keys);
        removeCount = removeCount == null ? 0 : removeCount;
        if (redisTemplate.hasKey(channelKey)) {
            this.setIfAbsent(timerKey,System.currentTimeMillis(),this.clearInvalidKeysInterval,TimeUnit.MILLISECONDS);
        }
        return removeCount;
    }

    @Override
    public long getDbIndex() {
        if (dbIndex == -1) {
            dbIndex = redisTemplate.execute(getDbIndexScript, Collections.emptyList());
        }
        return dbIndex;
    }

    @Override
    public boolean handleKeyExpiredMessage(String message) {
        String timerKeyPrefix = this.getTimerKeyPrefix();
        if (StringUtils.startsWith(message, timerKeyPrefix)) {
            long start = System.currentTimeMillis();
            String channel = StringUtils.substringAfter(message, timerKeyPrefix);
            Long removeCount = this.clearInvalidChannelKeys(channel);
            long end = System.currentTimeMillis();
            log.info("本次共移除频道[" + channel + "]的[" + removeCount + "]条无效key,用时:" + (end - start));
            return true;
        }
        return false;
    }

    public static DefaultRedisScript<Long> getClearInvalidKeysScript() {
        return clearInvalidKeysScript;
    }

    public static void setClearInvalidKeysScript(DefaultRedisScript<Long> clearInvalidKeysScript) {
        DefaultRedisCacheTemplate.clearInvalidKeysScript = clearInvalidKeysScript;
    }
}