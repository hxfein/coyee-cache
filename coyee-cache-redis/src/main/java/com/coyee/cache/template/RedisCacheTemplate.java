package com.coyee.cache.template;

import com.coyee.cache.bean.Data;
import com.coyee.cache.exception.CacheException;
import com.coyee.cache.store.ICacheTemplate;
import com.coyee.cache.utils.NetworkUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.*;
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
public class RedisCacheTemplate implements ICacheTemplate {
    private static Log log = LogFactory.getLog(RedisCacheTemplate.class);
    @Resource
    private RedisTemplate<Serializable, Serializable> redisTemplate;
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
     * 是否启用严格的一致性保护
     */
    private boolean strict = true;
    /**
     * 加锁重试次数
     */
    private int lockRetryTimes = 300;
    /**
     * 加锁失败重试时间间隔(毫秒)
     */
    private long lockRetrySleep = 50L;
    /**
     * 锁过期时间
     */
    private long lockExpireMillis = this.lockRetrySleep * this.lockRetryTimes;

    /**
     * 批量删除的方法
     */
    private Method deleteBatchMethod = null;
    /**
     * 删除单个的方法
     */
    private Method deleteSingleMethod = null;

    private static String localIp = NetworkUtils.getLocalHostLANIP();

    @PostConstruct
    private void init() {
        try {
            Class<?> clazz = redisTemplate.getClass();
            deleteBatchMethod = clazz.getMethod("delete", Collection.class);
            deleteSingleMethod = clazz.getMethod("delete", Object.class);
            redisTemplate.setEnableTransactionSupport(true);//启用事务
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
    private void deleteByKeys(Collection<Serializable> keys) {
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
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Serializable get(String key) {
        String storeKey = this.createKey(key);
        return redisTemplate.opsForValue().get(storeKey);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void clearChannelAndCache(String channel) {
        RedisCacheTemplate that = this;
        List<ChannelBlock> channelBlocks = getChannelBlocks(channel);
        this.doInLock(new DoInLockHandler() {
            @Override
            public void onLockOk(RedisOperations redisOperations) {
                try {
                    for (ChannelBlock channelBlock : channelBlocks) {
                        String channelKey = channelBlock.channelKey;
                        String channelLinkKey = channelBlock.linkKey;
                        Set<Serializable> keySet = channelBlock.keySet.stream().map(key -> that.createKey((String) key)).collect(Collectors.toSet());
                        that.deleteByKeys(keySet);//删除栏目相关的数据
                        that.deleteByKey(channelKey);//删除栏目
                        that.deleteByKey(channelLinkKey);//删除栏目关联
                    }
                } catch (Exception er) {
                    log.warn("清除栏目缓存出错,回滚事务", er);
                    if (strict == true) {
                        throw new CacheException("清除栏目缓存出错,严格模式下为避免脏数据产生,建议回滚事务", er);
                    }
                }
            }

            @Override
            public void onLockFailure() {
                if (strict == true) {
                    throw new CacheException("未能取得操作栏目[" + channel + "]的锁，不能清空缓存");
                }
            }
        }, channel);
    }

    /**
     * 获取一个栏目下的数据相关数据
     *
     * @param channel
     * @return
     */
    private List<ChannelBlock> getChannelBlocks(String channel) {
        List<ChannelBlock> channelBlocks = new ArrayList<>();
        String linkKey = this.createLinkKey(channel);
        Set<Serializable> linkChannels = redisTemplate.opsForSet().members(linkKey);
        for (Serializable linkChannel : linkChannels) {
            String channelKey = this.createChannelKey((String) linkChannel);
            String linkChannelKey = this.createLinkKey((String) linkChannel);
            Set<Serializable> keySet = redisTemplate.opsForSet().members(channelKey);
            ChannelBlock bean = new ChannelBlock();
            bean.channelKey = channelKey;
            bean.linkKey = linkChannelKey;
            bean.keySet = keySet;
            channelBlocks.add(bean);
        }
        return channelBlocks;
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void putChannelAndCache(String key, String[] channels, Serializable raw, long expires) {
        RedisCacheTemplate that = this;
        this.doInLock(new DoInLockHandler() {
            @Override
            public void onLockOk(RedisOperations redisOperations) {
                try {
                    String storeKey = that.createKey(key);
                    for (String channel : channels) {
                        String linkKey = that.createLinkKey(channel);
                        redisOperations.opsForSet().add(linkKey, channels);//建立栏目与其它栏目的关联关系
                        String channelKey = that.createChannelKey(channel);
                        redisOperations.opsForSet().add(channelKey, key);//建立栏目与缓存数据的关系，此处保留原始的key,不加前缀
                    }
                    redisOperations.opsForValue().set(storeKey, new Data(raw), expires, TimeUnit.MILLISECONDS);//保存实际缓存数据
                } catch (Exception er) {
                    log.error("存储栏目数据出错，回滚事务", er);
                }
            }

            @Override
            public void onLockFailure() {
                log.warn("未能取得操作栏目[" + Arrays.toString(channels) + "]的锁，不能存储数据");
            }
        }, channels);
    }

    /**
     * 获取锁
     *
     * @param channels
     * @return
     */
    public void doInLock(DoInLockHandler doInLockHandler, String... channels) {
        this.doInLock(doInLockHandler, this.lockRetryTimes, channels);
    }

    /**
     * 获取锁
     *
     * @param retryTimes 重新尝试次数
     * @param channels
     * @return
     */
    private void doInLock(DoInLockHandler doInLockHandler, int retryTimes, String... channels) {
        String threadSignature = this.createThreadSignature();
        Map<String, Serializable> params = new HashMap<>();
        for (String channel : channels) {
            String lockKey = this.createLockKey(channel);
            params.put(lockKey, threadSignature);
        }
        Boolean result = redisTemplate.opsForValue().multiSetIfAbsent(params);
        if (result == null) {
            throw new CacheException("redis未能响应加锁结果,请检查此lock()方法是否在redis事务代码中被调用!");
        }
        //成功取得锁
        if (result == true) {
            //设置锁的过期时间
            params.keySet().forEach(lockKey -> {
                redisTemplate.expire(lockKey, this.lockExpireMillis, TimeUnit.MILLISECONDS);
            });
            redisTemplate.execute(new SessionCallback<Object>() {
                @Override
                public <K, V> Object execute(RedisOperations<K, V> redisOperations) throws DataAccessException {
                    try {
                        redisOperations.multi();
                        doInLockHandler.onLockOk(redisOperations);
                        redisOperations.exec();
                    } catch (CacheException er) {
                        redisOperations.discard();
                        throw er;
                    }
                    return null;
                }
            });
        } else {
            retryTimes = retryTimes - 1;
            //再次尝试取得锁
            if (retryTimes > 0) {
                this.sleep(this.lockRetrySleep);
                log.warn("未能取得锁[" + Arrays.toString(channels) + "],进行第" + (this.lockRetryTimes - retryTimes) + "次尝试");
                doInLock(doInLockHandler, retryTimes, channels);
            } else {
                //多次尝试仍未取得锁
                doInLockHandler.onLockFailure();
            }
        }
        this.unlock(channels);
    }

    /**
     * 休眠一段时间
     *
     * @param millis
     */
    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException er) {
            //ignore exception
        }
    }


    /**
     * 主动释放由本线程创建的锁
     *
     * @param channels
     * @return
     */
    public void unlock(String... channels) {
        String threadSignature = this.createThreadSignature();
        for (String channel : channels) {
            String lockKey = this.createLockKey(channel);
            String value = (String) redisTemplate.opsForValue().get(lockKey);
            //只能解锁被本线程创建的锁
            if (StringUtils.equals(threadSignature, value)) {
                this.deleteByKey(lockKey);
            }
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

    /**
     * 创建锁存储键
     *
     * @param channel
     * @return
     */
    private String createLockKey(String channel) {
        return namespace + ":locks:" + channel;
    }

    /**
     * 创建锁的唯一标识符
     *
     * @return
     */
    private String createThreadSignature() {
        String threadName = Thread.currentThread().getName();
        long threadId = Thread.currentThread().getId();
        long hashCode = Thread.currentThread().hashCode();
        String value = localIp + "@" + threadName + "@" + threadId + "@" + hashCode;
        return value;
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

    public boolean isStrict() {
        return strict;
    }

    public void setStrict(boolean strict) {
        this.strict = strict;
    }

    public RedisTemplate<Serializable, Serializable> getRedisTemplate() {
        return redisTemplate;
    }

    public void setRedisTemplate(RedisTemplate<Serializable, Serializable> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public int getLockRetryTimes() {
        return lockRetryTimes;
    }

    public void setLockRetryTimes(int lockRetryTimes) {
        this.lockRetryTimes = lockRetryTimes;
    }

    public long getLockRetrySleep() {
        return lockRetrySleep;
    }

    public void setLockRetrySleep(long lockRetrySleep) {
        this.lockRetrySleep = lockRetrySleep;
    }

    public long getLockExpireMillis() {
        return lockExpireMillis;
    }

    public void setLockExpireMillis(long lockExpireMillis) {
        this.lockExpireMillis = lockExpireMillis;
    }

    /**
     * 定义栏目关联的关系
     */
    class ChannelBlock {
        String channelKey;
        String linkKey;
        Set<Serializable> keySet;
    }

    interface DoInLockHandler {
        /**
         * 加锁成功回调
         *
         * @param redisOperations
         */
        void onLockOk(RedisOperations redisOperations);

        /**
         * 加锁失败回调
         */
        default void onLockFailure() {

        }
    }
}
