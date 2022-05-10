package com.coyee.cache.generator;

import com.coyee.cache.utils.JSONUtils;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;

import java.lang.reflect.Method;

/**
 * @author hxfein
 * @className: Md5KeyGenerator
 * @description: MD5KEY生成器
 * @date 2022/4/27 16:09
 * @version：1.0
 */
public class CacheKeyGenerator {
    /**
     * 根据方法签名和参数生成md5 key
     *
     * @param clazz
     * @param method
     * @param params
     */
    public static String generateMD5Key(Class clazz, Method method, Object[] params) {
        String clazzName = clazz.getName();
        String methodName = method.getName();
        StringBuilder buffer = new StringBuilder();
        buffer.append(clazzName).append("#");
        buffer.append(methodName).append("#");
        Class[] parameterTypes = method.getParameterTypes();
        for (int i = 0; i < parameterTypes.length; i++) {
            String typeName = parameterTypes[i].getName();
            String paramString = JSONUtils.objectToString(params[i]);
            buffer.append(typeName).append("=").append(paramString);
        }
        String key = DigestUtils.md5Hex(buffer.toString());
        return key;
    }

    /**
     * 根据方法签名和参数生成调试key
     * @param clazz
     * @param method
     * @param params
     * @return
     */
    public static String generateDebugKey(Class clazz, Method method, Object[] params){
        String clazzName = clazz.getName();
        String methodName = method.getName();
        StringBuilder buffer = new StringBuilder();
        buffer.append(clazzName).append("#");
        buffer.append(methodName).append("#");
        Class[] parameterTypes = method.getParameterTypes();
        for (int i = 0; i < parameterTypes.length; i++) {
            String typeName = parameterTypes[i].getName();
            String paramString = JSONUtils.objectToString(params[i]);
            buffer.append(typeName).append("=").append(paramString);
        }
        return buffer.toString();
    }

}
