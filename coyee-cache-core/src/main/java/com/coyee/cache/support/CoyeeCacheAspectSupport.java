package com.coyee.cache.support;

import com.coyee.cache.bean.Data;
import com.coyee.cache.bean.KeyGenerator;
import com.coyee.cache.exception.CacheException;
import com.coyee.cache.generator.CacheKeyGenerator;
import com.coyee.cache.store.ICacheTemplate;
import com.coyee.cache.annotation.Cache;
import com.coyee.cache.annotation.Flush;
import com.coyee.cache.utils.JSONUtils;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.aspectj.MethodInvocationProceedingJoinPoint;
import org.springframework.context.expression.AnnotatedElementKey;
import org.springframework.expression.EvaluationContext;

import javax.annotation.Resource;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * 缓存管理器
 */
@Aspect
public class CoyeeCacheAspectSupport implements CoyeeCacheSupport {
    private static final Log log = LogFactory.getLog(CoyeeCacheAspectSupport.class);
    private ExpressionEvaluator evaluator = new ExpressionEvaluator();
    @Resource
    private ICacheTemplate cacheTemplate;

    /**
     * 判断是否为调试模式，若为测试模式，则key不会被md5存储
     *
     * @return
     */
    protected boolean isDebug() {
        return false;
    }

    /**
     * 拦截使用缓存的方法
     */
    @Pointcut("@annotation(com.coyee.cache.annotation.Cache)")
    private void cacheAnnotationPointCut() {
    }

    /**
     * 拦截刷新缓存的方法
     */
    @Pointcut("@annotation(com.coyee.cache.annotation.Flush)")
    private void flushAnnotationPointCut() {
    }

    /**
     * 使用缓存的处理逻辑
     *
     * @param jp
     * @return
     * @throws Throwable
     */
    @Around("cacheAnnotationPointCut()")
    public Object cacheAnnotationAround(ProceedingJoinPoint jp) throws Throwable {
        MethodInvocationProceedingJoinPoint methodJp = (MethodInvocationProceedingJoinPoint) jp;
        Cache config = this.getMethodAnnotation(methodJp, Cache.class);
        String key = createKey(methodJp, config);
        Object data = cacheTemplate.get(key);
        if (data != null) {
            if (data instanceof Data) {
                if (log.isDebugEnabled()) {
                    log.debug("使用key[" + key + "]从缓存中取得数据，直接返回");
                }
                return ((Data) data).getRawData();
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("使用key[" + key + "]未从缓存中取得数据，准备执行目标方法");
        }
        Serializable raw = (Serializable) jp.proceed();
        long expires = config.expire();
        if (log.isDebugEnabled()) {
            log.debug("使用key[" + key + "]设置缓存，过期时间:" + expires);
        }
        String[] channels = config.channels();
        cacheTemplate.putChannelAndCache(key, channels, raw, expires);
        return raw;
    }

    /**
     * 创建缓存key
     *
     * @param methodJp
     * @param config
     * @return
     */
    private String createKey(MethodInvocationProceedingJoinPoint methodJp, Cache config) {
        String keyTpl = config.key();
        if (StringUtils.isNotBlank(keyTpl)) {
            return createKeyFromTemplate(methodJp, keyTpl);
        } else {
            return this.createKeyFromGenerator(methodJp, config);
        }
    }

    /**
     * 解析表达式生成key
     *
     * @param methodJp
     * @param keyTpl
     * @return
     */
    private String createKeyFromTemplate(MethodInvocationProceedingJoinPoint methodJp, String keyTpl) {
        Object target = methodJp.getTarget();
        Class clazz = target.getClass();
        MethodSignature methodSignature = (MethodSignature) methodJp.getSignature();
        Method method = methodSignature.getMethod();
        Object[] args = methodJp.getArgs();
        EvaluationContext evaluationContext = evaluator.createEvaluationContext(target, clazz, method, args);
        AnnotatedElementKey methodKey = new AnnotatedElementKey(method, clazz);
        String key = (String) evaluator.condition(keyTpl, methodKey, evaluationContext, String.class);
        if (isDebug() == true) {
            return key;
        }
        return DigestUtils.md5Hex(key);
    }

    /**
     * 刷新缓存的处理逻辑
     *
     * @param jp
     * @return
     * @throws Throwable
     */
    @Around("flushAnnotationPointCut()")
    public Object flushAnnotationAround(ProceedingJoinPoint jp) throws Throwable {
        MethodInvocationProceedingJoinPoint methodJp = (MethodInvocationProceedingJoinPoint) jp;
        Flush config = this.getMethodAnnotation(methodJp, Flush.class);
        String[] channels = config.channels();
        String execOrder = config.execOrder();
        if (StringUtils.equals(execOrder, "before")) {
            this.flushChannelKeysAndCache(channels);
            if (log.isDebugEnabled()) {
                log.debug("在目标方法执行前 清理频道数据：[" + JSONUtils.objectToString(channels) + "]");
            }
        }
        Object data = jp.proceed();
        if (StringUtils.equals(execOrder, "after")) {
            this.flushChannelKeysAndCache(channels);
            if (log.isDebugEnabled()) {
                log.debug("在目标方法执行后 清理频道数据：[" + JSONUtils.objectToString(channels) + "]");
            }
        }
        return data;
    }

    /**
     * 清空channel和key的映射关系，以及缓存的数据
     *
     * @param channels
     */
    public void flushChannelKeysAndCache(String[] channels) {
        if (channels == null) {
            return;
        }
        for (String channel : channels) {
            cacheTemplate.clearChannelAndCache(channel);
        }
    }


    /**
     * 通过生成器生成缓存key
     *
     * @param methodJp
     * @param config
     * @return
     */
    private String createKeyFromGenerator(MethodInvocationProceedingJoinPoint methodJp, Cache config) {
        String key = config.key();
        if (StringUtils.isBlank(key)) {
            KeyGenerator keyGenerator = config.keyGenerator();
            MethodSignature methodSignature = (MethodSignature) methodJp.getSignature();
            Object target = methodJp.getTarget();
            Class clazz = target.getClass();
            Method method = methodSignature.getMethod();
            Object[] params = methodJp.getArgs();
            String logMethodString = String.format("方法:%s@%s", clazz.getName(), method.getName());
            String paramsString = JSONUtils.objectToString(params);
            String logParamsString = String.format("参数:%s", paramsString);
            if (isDebug()) {
                key = CacheKeyGenerator.generateDebugKey(clazz, method, params);
                if (log.isDebugEnabled()) {
                    log.debug(logMethodString + " , " + logParamsString + " , 生成key:" + key);
                }
            } else if (keyGenerator == KeyGenerator.Signature) {
                key = CacheKeyGenerator.generateMD5Key(clazz, method, params);
                if (log.isDebugEnabled()) {
                    log.debug(logMethodString + " , " + logParamsString + " , 生成key:" + key);
                }
            } else {
                throw new CacheException("未找到key或keyGenerator");
            }
        }
        return key;
    }

    /**
     * 获取方法上的注解
     *
     * @param methodJp
     * @param annotationClazz
     * @param <T>
     * @return
     * @throws NoSuchMethodException
     */
    private <T extends Annotation> T getMethodAnnotation(MethodInvocationProceedingJoinPoint methodJp, Class<T> annotationClazz) throws NoSuchMethodException {
        MethodSignature methodSignature = (MethodSignature) methodJp.getSignature();
        Object target = methodJp.getTarget();
        Method method = methodSignature.getMethod();
        Class<?> parameterTypes[] = method.getParameterTypes();
        Class<?> targetClass = target.getClass();
        String methodName = method.getName();
        Method targetMethod = targetClass.getMethod(methodName, parameterTypes);
        return targetMethod.getAnnotation(annotationClazz);
    }

}
