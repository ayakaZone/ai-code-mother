package com.neko.nekoaicodemother;

import dev.langchain4j.community.store.embedding.redis.spring.RedisEmbeddingStoreAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

// 排除RedisEmbeddingStoreAutoConfiguration启动类加载保证正常启动
@SpringBootApplication(exclude = {RedisEmbeddingStoreAutoConfiguration.class})
// 暴露 AOP 切面代理对象
@EnableAspectJAutoProxy(exposeProxy = true)
// Mybatis Mapper 扫描
@MapperScan("com.neko.nekoaicodemother.mapper")
// 支持 Spring Data Redis 缓存注解
@EnableCaching
public class NekoAiCodeMotherApplication {

    public static void main(String[] args) {
        SpringApplication.run(NekoAiCodeMotherApplication.class, args);
    }

}
