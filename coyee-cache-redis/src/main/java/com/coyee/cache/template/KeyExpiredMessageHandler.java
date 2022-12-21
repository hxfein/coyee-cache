package com.coyee.cache.template;

import org.springframework.data.redis.connection.MessageListener;

/**
 * @author hxfein
 * @className: ExpiredMessageListener
 * @description: key过期listener
 * @date 2022/12/19 14:35
 * @version：1.0
 */
public interface KeyExpiredMessageHandler extends MessageListener {
}
