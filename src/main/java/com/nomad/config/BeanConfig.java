package com.nomad.config;

import org.apache.commons.dbcp.BasicDataSource;
import org.springframework.cache.CacheManager;
//下面的CacheManager不能import，要使用new net.sf.ehcache.CacheManager()
//import net.sf.ehcache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.ehcache.EhCacheCacheManager;
import org.springframework.cache.ehcache.EhCacheManagerFactoryBean;
import org.springframework.cache.support.CompositeCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.jndi.JndiObjectFactoryBean;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;

@Configuration
@ComponentScan(basePackages = "com.nomad")
//spring本身没有实现缓存解决方案，对缓存功能提供了声明式的支持，集成第三方流行的缓存框架
//启用spring对注解驱动缓存的支持 或者 <cache:annotation-driven/>
@EnableCaching
public class BeanConfig {

    /*有多种缓存管理器，比如ConcurrentMapCacheManager、SimpleCacheManager、RedisCacheManger...*/
    //1.ConcurrentMapCacheManager
    @Bean
    public ConcurrentMapCacheManager cacheManager1() {
        return new ConcurrentMapCacheManager(); //基于内存的
    }

    //2.Ehcache缓存
    @Bean
    public EhCacheCacheManager cacheManager2(net.sf.ehcache.CacheManager cm) {
        return new EhCacheCacheManager(cm);
    }
    @Bean
    public EhCacheManagerFactoryBean ehcache() {
        EhCacheManagerFactoryBean ehCacheManagerFactoryBean =
                new EhCacheManagerFactoryBean();
        ehCacheManagerFactoryBean.setConfigLocation(
                new ClassPathResource("com/nomad/cache/ehcache.xml")
        );
        return ehCacheManagerFactoryBean;
    }

    //3.Redis缓存
    @Bean
    public RedisCacheManager cacheManager3(RedisTemplate redisTemplate) {
        return new RedisCacheManager(redisTemplate);
    }

    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory redisCF) {
        RedisTemplate<String, String> redisTemplate = new RedisTemplate<String, String>();
        redisTemplate.setConnectionFactory(redisCF);
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

    @Bean
    public JedisConnectionFactory redisConnectionFactory() {
        JedisConnectionFactory jedisConnectionFactory =
                new JedisConnectionFactory();
        jedisConnectionFactory.afterPropertiesSet();
        return jedisConnectionFactory;
    }


    //4.使用多个缓存管理器   缓存会按添加的顺序使用
    @Bean
    public CacheManager cacheManager4(
            ConcurrentMapCacheManager cm1,
            EhCacheCacheManager cm2,
            RedisCacheManager cm3
    ) {
        CompositeCacheManager cacheManager = new CompositeCacheManager();
        List<CacheManager> managers = new ArrayList<CacheManager>();
        managers.add(cm1);
        managers.add(cm2);
        managers.add(cm3);
        cacheManager.setCacheManagers(managers);
        return cacheManager;
    }


    /*配置数据源（有多种方式，如jndi、连接池、jdbc驱动、嵌入式数据源）和jdbctemplate(处理连接开启关闭和异常转换)*/
    //1.jndi数据源
    //@Profile("production")
    @Bean
    public JndiObjectFactoryBean dataSource1() {
        JndiObjectFactoryBean jndiObjectFB = new JndiObjectFactoryBean();
        jndiObjectFB.setJndiName("jdbc/test");
        jndiObjectFB.setResourceRef(true);
        jndiObjectFB.setProxyInterface(javax.sql.DataSource.class);
        return jndiObjectFB;
    }

    //2.数据源连接尺 整合 dbcp、c3p0、bonecp。。。
    //@Profile("qa")
    @Bean
    public BasicDataSource dataSource2() {
        BasicDataSource ds = new BasicDataSource();
        ds.setDriverClassName("org.h2.Driver");
        ds.setUrl("jdbc:h2:tcp://localhost/~/user");
        ds.setUsername("test");
        ds.setPassword("");
        ds.setInitialSize(5);
        ds.setMaxActive(10);
        return ds;
    }

    //3.基于jdbc驱动的数据源 spring提供的 3个
    @Bean
    public DriverManagerDataSource dataSource3() {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName("org.h2.Driver");
        ds.setUrl("jdbc:h2:tcp://localhost/~/user");
        ds.setUsername("test");
        ds.setPassword("");
        return ds;
    }

    //4.嵌入式数据源 基于内存 应用绑定
    //@Profile("development")
    @Bean
    public EmbeddedDatabase dataSource4() {
        return new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .addScript("classpath:com/nomad/sql/schema.sql")
                .addScript("classpath:com/nomad/sql/test-data.sql")
                .build();
    }

    @Bean
    public JdbcTemplate jdbcTemplate(EmbeddedDatabase ds) {
        return new JdbcTemplate(ds);
    }

}
