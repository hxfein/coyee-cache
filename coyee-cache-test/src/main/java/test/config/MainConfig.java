package test.config;

import com.coyee.cache.store.ICacheTemplate;
import com.coyee.cache.support.CoyeeCacheAspectSupport;
import com.coyee.cache.template.RedisWithLockCacheTemplate;
import com.coyee.cache.template.RedisWithoutLockCacheTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Component
@Configuration
public class MainConfig {

    @Bean
    public ICacheTemplate cacheTemplate(){
        return new RedisWithoutLockCacheTemplate();
    }




    @Bean
    public CoyeeCacheAspectSupport coyeeCacheAspectSupport(){
        CoyeeCacheAspectSupport coyeeCacheAspectSupport= new CoyeeCacheAspectSupport();
        coyeeCacheAspectSupport.setMinFlushInterval(1000);
        return coyeeCacheAspectSupport;
    }
}