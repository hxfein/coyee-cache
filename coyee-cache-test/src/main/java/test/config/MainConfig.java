package test.config;

import com.coyee.cache.support.CoyeeCacheAspectSupport;
import com.coyee.cache.store.ICacheTemplate;
import com.coyee.cache.store.MapCacheTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@Configuration
@EnableAspectJAutoProxy//开启基于注解的AOP模式
@ComponentScan(basePackages={"test","com.coyee"})
public class MainConfig {
    @Bean
    public ICacheTemplate cacheTemplate(){
        return new MapCacheTemplate();
    }

    @Bean
    public CoyeeCacheAspectSupport cacheManager(){
        return new CoyeeCacheAspectSupport();
    }
}