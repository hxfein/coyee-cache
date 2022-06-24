package com.coyee.cache.template;

import com.coyee.cache.bean.Data;
import com.coyee.cache.exception.CacheException;
import com.coyee.cache.store.ICacheTemplate;
import com.coyee.cache.utils.NetworkUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author hxfein
 * @className: RedisCacheTemplate
 * @description: putChannelAndCache方法用于将数据保存在redis中并建立缓存与栏目的关系。
 * clearChannelAndCache方法用于清除缓存和缓存相关的关系。
 * 为保证数据一致性，这两个方法均采用事务方式尽可能避免脏数据的产生。
 * <p>
 * 一般而言，putChannelAndCache方法是业务方法获取到数据以后才被调用，此时产生脏数据的可能性较小，
 * 只是会造成缓存没有生成的情况，因此即使在严格模式下存储出错仍然不影响业务代码的执行。
 * <p>
 * 而clearChannelAndCache一般是由务数据发生变更后才会被调用，此时如果缓存数据没有被清除，
 * 会导致业务数据与缓存数据不一致，进而造成业务错误，因此在严格模式下此种情况直接抛出异常由业务上决定是否回滚。
 * @date 2022/4/25 16:04
 * @version：1.0
 */
public class RedisWithoutLockCacheTemplate extends AbstractRedisCacheTemplate {
    private static Log log = LogFactory.getLog(RedisWithoutLockCacheTemplate.class);

    @Override
    public Serializable get(String key) {
        String storeKey = this.createKey(key);
        return redisTemplate.opsForValue().get(storeKey);
    }

    @Override
    public void clearChannelAndCache(String channel) {
        RedisWithoutLockCacheTemplate that = this;
        List<RedisWithoutLockCacheTemplate.ChannelBlock> channelBlocks = getChannelBlocks(channel);
        if (channelBlocks.isEmpty()) {
            return;
        }
        for (RedisWithoutLockCacheTemplate.ChannelBlock channelBlock : channelBlocks) {
            String channelKey = channelBlock.channelKey;
            String channelLinkKey = channelBlock.linkKey;
            Set<Serializable> keySet = channelBlock.keySet.stream().map(key -> that.createKey((String) key)).collect(Collectors.toSet());
            that.deleteByKeys(keySet);//删除栏目相关的数据
            that.deleteByKey(channelKey);//删除栏目
            that.deleteByKey(channelLinkKey);//删除栏目关联
        }
    }

    /**
     * 获取一个栏目下的数据相关数据
     *
     * @param channel
     * @return
     */
    private List<RedisWithoutLockCacheTemplate.ChannelBlock> getChannelBlocks(String channel) {
        List<RedisWithoutLockCacheTemplate.ChannelBlock> channelBlocks = new ArrayList<>();
        String linkKey = this.createLinkKey(channel);
        Set<Serializable> linkChannels = redisTemplate.opsForSet().members(linkKey);
        for (Serializable linkChannel : linkChannels) {
            String channelKey = this.createChannelKey((String) linkChannel);
            String linkChannelKey = this.createLinkKey((String) linkChannel);
            Set<Serializable> keySet = redisTemplate.opsForSet().members(channelKey);
            RedisWithoutLockCacheTemplate.ChannelBlock bean = new RedisWithoutLockCacheTemplate.ChannelBlock();
            bean.channelKey = channelKey;
            bean.linkKey = linkChannelKey;
            bean.keySet = keySet;
            channelBlocks.add(bean);
        }
        return channelBlocks;
    }

    @Override
    public void putChannelAndCache(String key, String[] channels, Serializable raw, long expires) {
        for (String channel : channels) {
            for (String linkChannel : channels) {
                String linkKey = this.createLinkKey(linkChannel);
                redisTemplate.opsForSet().add(linkKey, channels);//建立栏目与其它栏目的关联关系
                String channelKey = this.createChannelKey(channel);
                redisTemplate.opsForSet().add(channelKey, key);//建立栏目与缓存数据的关系，此处保留原始的key,不加前缀
            }
        }
        String storeKey = this.createKey(key);
        redisTemplate.opsForValue().set(storeKey, new Data(raw), expires, TimeUnit.MILLISECONDS);//保存实际缓存数据
    }

}