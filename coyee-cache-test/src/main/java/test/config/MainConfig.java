package test.config;

import com.coyee.cache.support.CoyeeCacheAspectSupport;
import com.coyee.cache.template.DefaultRedisCacheTemplate;
import com.coyee.cache.template.KeyExpiredMesssageListenerContainer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.Serializable;

@Component
@Configuration
public class MainConfig {
    @Resource
    private RedisConnectionFactory redisConnectionFactory;
    @Resource
    private RedisTemplate<Serializable, Serializable> redisTemplate;
    @Bean
    public DefaultRedisCacheTemplate getRedisCacheTemplate(){
        return new DefaultRedisCacheTemplate(redisConnectionFactory,redisTemplate);
    }
    @Bean
    public RedisMessageListenerContainer getMessageListenerContainer(){
        DefaultRedisCacheTemplate cacheTemplate=this.getRedisCacheTemplate();
        KeyExpiredMesssageListenerContainer listenerContainer = new KeyExpiredMesssageListenerContainer(redisConnectionFactory,cacheTemplate,11);
        return listenerContainer;
    }

    @Bean
    public CoyeeCacheAspectSupport coyeeCacheAspectSupport(){
        CoyeeCacheAspectSupport coyeeCacheAspectSupport= new CoyeeCacheAspectSupport();
        coyeeCacheAspectSupport.setMinFlushInterval(1000);
        return coyeeCacheAspectSupport;
    }
}