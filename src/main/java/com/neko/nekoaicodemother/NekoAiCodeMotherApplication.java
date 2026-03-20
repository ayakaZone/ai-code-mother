package com.neko.nekoaicodemother;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@EnableAspectJAutoProxy(exposeProxy = true)
public class NekoAiCodeMotherApplication {

    public static void main(String[] args) {
        SpringApplication.run(NekoAiCodeMotherApplication.class, args);
    }

}
