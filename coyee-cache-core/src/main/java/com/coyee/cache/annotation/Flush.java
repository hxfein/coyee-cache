package com.coyee.cache.annotation;

import java.lang.annotation.*;

/**
 * @author hxfein
 * @className: CyCache
 * @description: 标记业务方法使用缓存
 * @date 2022/4/25 11:21
 * @version：1.0
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Flush {
    /**
     * 指定该刷新操作影响的频道
     * @return
     */
    String[] channels() default "";

    /**
     * 执行顺序（after：在方法调用后，before:在方法调用前）
     * @return
     */
    String execOrder() default "after";
}
