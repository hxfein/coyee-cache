package com.coyee.cache.annotation;

import java.lang.annotation.*;

/**
 * @author hxfein
 * @className: Update
 * @description:
 * 用于标识mapper方法的执行是否更新缓存
 *
 * @date 2022/6/24 16:00
 * @version：1.0
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Update {
    /**
     * 是否刷新缓存
     * cache为false则表示在mapper执行前缓存已被更新，mapper方法的执行不再主动刷新缓存
     * cache为true表示mapper执行后需刷新缓存
     * @return
     */
    boolean cache() default true;
}
