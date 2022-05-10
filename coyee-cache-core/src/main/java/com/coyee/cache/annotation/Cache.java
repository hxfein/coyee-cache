package com.coyee.cache.annotation;
import com.coyee.cache.bean.KeyGenerator;

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
public @interface Cache {
    /**
     * 缓存KEY,若指定了keygenerator则该设置无效
     * @return
     */
    String key() default "";

    /**
     * 监听数据
     * @return
     */
    String[] channels() default "";

    /**
     * 过期时间
     * @return
     */
    int expire() default 5*60*1000;

    /**
     * key生成器,默认通过方法签名生成
     * @return
     */
    KeyGenerator keyGenerator() default KeyGenerator.Signature;
}
