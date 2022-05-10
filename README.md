# 缓存关系管理工具包

## 介绍
用于解决使用了缓存的JAVA项目中普遍存在的数据对象互相影响，数据对象的变化难以及时的、容易的影响到被缓存的数据对象问题。  
  
例如：方法1引用了A、B、C三张表的数据，方法2引用了B、D两张表的数据，方法3引用了A、C两张表的数据，当其中某个表的数据发生变化时，传统的方法难以控制各方法的缓存失效。  
  
本项目致力于解决以上问题的思路是每个获取数据的方法只关心自己用了哪些表的数据，而更新数据的方法只关心自己改了哪些表的数据，由容器自动维护数据维护方和数据使用方的关系，简化缓存的管理工作。  

## 工程说明
```java 
coyee-cache-core  主要功能包,默认采用内置的map缓存实现
coyee-cache-redis 采用redis作为缓存存储时使用的包
coyee-cache-mybatis 监听mybatis更新表事件，自动更新缓存的包
coyee-cache-test  测试代码
```


## 使用说明
1.  将本项目打包上传到私有仓库中
2.  业务工程中引入coyee-cache-core包，根据情况决定是否引用coyee-cache-redis和coyee-cache-mybatis
3.  在工程中添加如下配置代码:
````java
@Configuration
@Component
public class CoyeeCacheConfig extends CoyeeCacheAspectSupport {
    /**
     * 使用redis作为缓存实现
     * @return
     */
    @Bean
    public ICacheTemplate cacheTemplate() {
        return new RedisCacheTemplate();
    }

    /**
     * 关闭调试模式
     * @return
     */
    @Override
    public boolean isDebug() {
        return false;
    }

    /**
     * 开启mybatis更新数据SQL监听，同步维护缓存
     * @return
     */
    @Bean
    public CoyeeCacheFlushInterceptor coyeeCacheFlushInterceptor() {
        return new CoyeeCacheFlushInterceptor(this);
    }
}
````
4.  在方法中添加管理缓存的注解代码

####使用缓存
```java 
//代表该方法返回的数据涉及A,B,C三张表
@Cache(channels={"A","B","C"})
public R<IPage<Info>> paging(InfoDTO info, Query query);
```

####更新缓存
```java 
//代表该方法会更新C表的数据，引用C表数据的所有缓存将会失效
@Flush(channels={"C"})
public R save(@Valid @RequestBody Info info);
```
## 存在问题
1.  缓存管理的颗粒度目前只到表，若某个表频繁发生数据变化会导致缓存频繁失效，产生负面效果。
2.  若一个表的数据发生变化，而与该表相关联的数据过多，会产生缓存维护时间过长的问题。



