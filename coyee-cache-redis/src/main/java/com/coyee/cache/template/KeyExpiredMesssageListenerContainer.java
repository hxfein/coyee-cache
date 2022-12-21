package com.coyee.cache.template;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

/**
 * @author hxfein
 * @className: KeyExpiredMesssageListenerContainer
 * @description: 过期事件监听容器
 * @date 2022/12/19 14:23
 * @version：1.0
 */
public class KeyExpiredMesssageListenerContainer extends RedisMessageListenerContainer {
    private Logger logger = LoggerFactory.getLogger(KeyExpiredMesssageListenerContainer.class);
    /**
     * 使用的数据库
     */
    private int dbIndex;

    private AbstractRedisCacheTemplate redisCacheTemplate;
    /**
     *
     */
    private List<KeyExpiredMessageHandler> messageHandlers;

    public KeyExpiredMesssageListenerContainer(RedisConnectionFactory connectionFactory, AbstractRedisCacheTemplate redisCacheTemplate, int dbIndex) {
        this(connectionFactory, redisCacheTemplate, Collections.emptyList(), dbIndex);
    }

    public KeyExpiredMesssageListenerContainer(RedisConnectionFactory connectionFactory, AbstractRedisCacheTemplate redisCacheTemplate) {
        this(connectionFactory, redisCacheTemplate, Collections.emptyList(),-1);
        this.dbIndex=(int)redisCacheTemplate.getDbIndex();
    }

    public KeyExpiredMesssageListenerContainer(RedisConnectionFactory connectionFactory, AbstractRedisCacheTemplate redisCacheTemplate, List<KeyExpiredMessageHandler> messageHandlers) {
        this(connectionFactory, redisCacheTemplate, messageHandlers,-1);
        this.dbIndex=(int)redisCacheTemplate.getDbIndex();
    }

    public KeyExpiredMesssageListenerContainer(RedisConnectionFactory connectionFactory, AbstractRedisCacheTemplate redisCacheTemplate, List<KeyExpiredMessageHandler> messageHandlers, int dbIndex) {
        super.setConnectionFactory(connectionFactory);
        this.redisCacheTemplate = redisCacheTemplate;
        this.messageHandlers = messageHandlers;
        this.dbIndex = dbIndex;
    }


    public AbstractRedisCacheTemplate getRedisCacheTemplate() {
        return redisCacheTemplate;
    }

    public void setRedisCacheTemplate(AbstractRedisCacheTemplate redisCacheTemplate) {
        this.redisCacheTemplate = redisCacheTemplate;
    }

    public int getDbIndex() {
        return dbIndex;
    }

    public void setDbIndex(int dbIndex) {
        this.dbIndex = dbIndex;
    }

    public List<KeyExpiredMessageHandler> getMessageHandlers() {
        return messageHandlers;
    }

    public void setMessageHandlers(List<KeyExpiredMessageHandler> messageHandlers) {
        this.messageHandlers = messageHandlers;
    }

    @Override
    public void afterPropertiesSet() {
        super.afterPropertiesSet();
        RedisConnection connection = getConnectionFactory().getConnection();
        try {
            Properties config = this.getConfig(connection, "notify-keyspace-events");
            if (config == null) {
                logger.warn("你使用的redis api不支持自动开启通知选项，请手动开启");
            }else {
                if (!StringUtils.isBlank(config.getProperty("notify-keyspace-events"))) {
                    connection.setConfig("notify-keyspace-events", "KEA");
                }
            }
        } finally {
            connection.close();
        }

        PatternTopic listenTopic = new PatternTopic("__keyevent@" + dbIndex + "__:expired");
        this.addMessageListener((msg, bytes) ->
        {
            String message = msg.toString();
            boolean processed = redisCacheTemplate.handleKeyExpiredMessage(message);
            if (processed == true) {
                return;
            }
            for (KeyExpiredMessageHandler messageHandler : messageHandlers) {
                messageHandler.onMessage(msg, bytes);
            }
        }, listenTopic);
    }

    private Properties getConfig(RedisConnection connection, String configName) {
        try {
            Method method = RedisConnection.class.getMethod("getConfig", String.class);
            Object configObject = method.invoke(connection, configName);
            if (configObject != null && configObject instanceof Properties) {
                return (Properties) configObject;
            }
            return null;
        } catch (Exception er) {
            logger.debug("获取配置失败", er);
            return null;
        }
    }
}
