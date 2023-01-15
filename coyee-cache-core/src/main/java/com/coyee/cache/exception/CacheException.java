package com.coyee.cache.exception;

/**
 * @author hxfein
 * @className: CacheException
 * @description: 缓存业务异常类
 * @date 2022/4/27 16:25
 * @version：1.0
 */
public class CacheException extends RuntimeException {

    public CacheException(String msg) {
        super(msg);
    }

    public CacheException(String msg, Throwable throwable) {
        super(msg, throwable);
    }

}
