package com.neko.nekoaicodemother;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@EnableAspectJAutoProxy(exposeProxy = true)
@MapperScan("com.neko.nekoaicodemother.mapper")
public class NekoAiCodeMotherApplication {

    public static void main(String[] args) {
        SpringApplication.run(NekoAiCodeMotherApplication.class, args);
    }

}
