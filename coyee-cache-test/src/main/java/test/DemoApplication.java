package test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@EnableAspectJAutoProxy//开启基于注解的AOP模式
@EnableCaching
@ComponentScan(basePackages = {"test", "com.coyee"})
public class DemoApplication {
    static public void main(String[] args) throws Exception {
        SpringApplication.run(DemoApplication.class, args);
    }
}