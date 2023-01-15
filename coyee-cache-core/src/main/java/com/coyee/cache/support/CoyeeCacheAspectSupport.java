package com.coyee.cache.support;

import com.coyee.cache.annotation.Cache;
import com.coyee.cache.annotation.Flush;
import com.coyee.cache.bean.Data;
import com.coyee.cache.bean.KeyGenerator;
import com.coyee.cache.exception.CacheException;
import com.coyee.cache.generator.CacheKeyGenerator;
import com.coyee.cache.store.ICacheTemplate;
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

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * 缓存管理器
 */
@Aspect
public class CoyeeCacheAspectSupport implements CoyeeCacheSupport {
    private static final Log log = LogFactory.getLog(CoyeeCacheAspectSupport.class);
    private ExpressionEvaluator evaluator = new ExpressionEvaluator();
    private long queryCount = 0;
    private long hitCount = 0;
    @Resource
    private ICacheTemplate cacheTemplate;
    /**
     * 是否启用环境管理
     */
    private boolean disabled = false;
    /**
     * 记录频道的上次刷新时间
     */
    private Map<String, Long> lastFlushMills = new HashMap<>();
    /**
     * 最小刷新间隔，若小于此间隔将不刷新
     */
    private long minFlushInterval = 300;
    /**
     * 默认过期时间：10分钟
     */
    private long defaultExpires = 1000 * 60 * 10;

    @PostConstruct
    private void init() {
        String cacheDisabled = System.getenv("COYEE_CACHE_DISABLED");
        this.disabled = StringUtils.equalsIgnoreCase(cacheDisabled, "true");
    }

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
        if (this.disabled == true) {
            return methodJp.proceed();
        }
        this.queryCount++;
        Cache config = this.getMethodAnnotation(methodJp, Cache.class);
        //执行前置方法
        this.executeBeforeHandler(methodJp, config);
        //执行前缀方法结束
        String key = createKey(methodJp, config);
        log.debug("处理[" + methodJp.getSignature() + "]的获取缓存请求,当前key为:" + key);
        Object data = cacheTemplate.get(key);
        if (data != null) {
            if (data instanceof Data) {
                if (log.isDebugEnabled()) {
                    log.debug("使用key[" + key + "]从缓存中取得数据，直接返回");
                }
                Serializable result = ((Data) data).getRawData();
                result = this.executeAfterHandler(methodJp, config, result);
                this.hitCount++;
                return result;
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("使用key[" + key + "]未从缓存中取得数据，准备执行目标方法");
        }
        Serializable raw = (Serializable) jp.proceed();
        raw = this.executeAfterHandler(methodJp, config, raw);
        long expires = config.expire();
        expires = expires == -1 ? defaultExpires : expires;//如果未设置过期时间就采用默认过期时间
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
        if (this.disabled == true) {
            return methodJp.proceed();
        }
        log.debug("处理[" + methodJp.getSignature() + "]的刷新缓存请求。。。");
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
            Long lastFlushMills = this.lastFlushMills.get(channel);
            lastFlushMills = lastFlushMills == null ? 0L : lastFlushMills;
            long currentMills = System.currentTimeMillis();
            long flushInterval = currentMills - lastFlushMills;
            if (flushInterval > minFlushInterval) {
                cacheTemplate.clearChannelAndCache(channel);
                currentMills = System.currentTimeMillis();
                this.lastFlushMills.put(channel, currentMills);
            } else {
                log.debug("[" + channel + "]的刷新间隔过低，本次不刷新。");
            }
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
            if (isDebug()) {
                key = CacheKeyGenerator.generateDebugKey(clazz, method, params);
            } else if (keyGenerator == KeyGenerator.Signature) {
                key = CacheKeyGenerator.generateMD5Key(clazz, method, params);
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

    /**
     * 执行前置方法
     *
     * @param methodJp
     * @param config
     * @throws NoSuchMethodException
     */
    private void executeBeforeHandler(MethodInvocationProceedingJoinPoint methodJp, Cache config) throws NoSuchMethodException {
        MethodSignature methodSignature = (MethodSignature) methodJp.getSignature();
        Object target = methodJp.getTarget();
        Class<?> targetClass = target.getClass();
        Method method = methodSignature.getMethod();
        String beforeExec = config.beforeExec();
        if (StringUtils.isBlank(beforeExec)) {
            return;
        }
        Method beforeMethod = targetClass.getMethod(beforeExec, Object[].class);
        if (beforeMethod == null) {
            throw new CacheException("方法[" + targetClass.getName() + "." + method + "]配置了缓存前缀执行方法，但没有找到该方法!");
        }
        Object[] args = methodJp.getArgs();
        try {
            beforeMethod.invoke(target, new Object[]{args});
        } catch (Exception er) {
            throw new CacheException("执行前缀方法[" + targetClass.getName() + "." + method + "]出错", er);
        }
    }

    /**
     * 执行后置方法
     *
     * @param methodJp
     * @param config
     * @throws NoSuchMethodException
     */
    private Serializable executeAfterHandler(MethodInvocationProceedingJoinPoint methodJp, Cache config, Serializable result) throws NoSuchMethodException {
        MethodSignature methodSignature = (MethodSignature) methodJp.getSignature();
        Object target = methodJp.getTarget();
        Class<?> targetClass = target.getClass();
        Method method = methodSignature.getMethod();
        String afterExec = config.afterExec();
        if (StringUtils.isBlank(afterExec)) {
            return result;
        }
        Method afterMethod = targetClass.getMethod(afterExec, Object[].class, Serializable.class);
        if (afterMethod == null) {
            throw new CacheException("方法[" + targetClass.getName() + "." + method + "]配置了缓存后置执行方法，但没有找到该方法!");
        }
        Object[] args = methodJp.getArgs();
        try {
            result = (Serializable) afterMethod.invoke(target, new Object[]{args, result});
            return result;
        } catch (Exception er) {
            throw new CacheException("执行后置方法[" + targetClass.getName() + "." + method + "]出错", er);
        }
    }

    public long getMinFlushInterval() {
        return minFlushInterval;
    }

    public void setMinFlushInterval(long minFlushInterval) {
        this.minFlushInterval = minFlushInterval;
    }

    public long getDefaultExpires() {
        return defaultExpires;
    }

    public void setDefaultExpires(long defaultExpires) {
        this.defaultExpires = defaultExpires;
    }

    @Override
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("queryCount", queryCount);
        stats.put("hitCount", hitCount);
        double percent = -1;
        if (this.queryCount > 0) {
            percent = (double) this.hitCount / (double) this.queryCount;
        }
        stats.put("percent", percent);
        return stats;
    }
}
