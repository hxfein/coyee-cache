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

    /**
     * 进入方法时回调的方法名,方法签名参考:
     * void beforeTest(Object[] params);
     * 其中:
     * params与拦截方法的参数列表一致
     * 便于用户在取得缓存数据前更新动态数据，例如获取商品数据之前可以更新商品的浏览量
     * @return
     */
    String beforeExec() default "";

    /**
     * 离开方法时回调的方法名，方法签名参考:
     * Serializable afterTest(Object[] params,Serializable result)
     * 其中:
     * params与拦截方法的参数列表一致
     * result为最终返回的数据
     * 用户可在此回调中修改缓存数据动态的部分，例如从缓存中取得商品数据后要把商品的浏览量修改为实时数据
     * @return
     */
    String afterExec() default "";
}
