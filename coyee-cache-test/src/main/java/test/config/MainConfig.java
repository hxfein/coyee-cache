package test.config;

import com.coyee.cache.store.ICacheTemplate;
import com.coyee.cache.support.CoyeeCacheAspectSupport;
import com.coyee.cache.template.RedisCacheTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Component
@Configuration
public class MainConfig {

    @Bean
    public ICacheTemplate cacheTemplate(){
        return new RedisCacheTemplate();
    }




    @Bean
    public CoyeeCacheAspectSupport coyeeCacheAspectSupport(){
        return new CoyeeCacheAspectSupport();
    }
}