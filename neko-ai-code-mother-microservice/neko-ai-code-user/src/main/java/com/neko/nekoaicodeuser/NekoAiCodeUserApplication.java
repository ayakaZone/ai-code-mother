package com.neko.nekoaicodeuser;

//import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@MapperScan("com.neko.nekoaicodeuser.mapper")
@ComponentScan("com.neko")
//@EnableDubbo
public class NekoAiCodeUserApplication {

    public static void main(String[] args) {
        SpringApplication.run(NekoAiCodeUserApplication.class, args);
    }
}
