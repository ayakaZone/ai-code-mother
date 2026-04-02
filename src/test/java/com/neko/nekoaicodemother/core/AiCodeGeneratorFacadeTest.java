package com.neko.nekoaicodemother.core;

import com.neko.nekoaicodemother.model.enums.CodeGenTypeEnum;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;

import java.io.File;
import java.util.List;

@SpringBootTest
class AiCodeGeneratorFacadeTest {

    @Resource
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;

    @Test
    void generatorAndSave() {
        File file = aiCodeGeneratorFacade.GeneratorAndSave
                ("生成一个个人主页页面，要求代码在20行内", CodeGenTypeEnum.MULTI_FILE, 1L);
        Assertions.assertNotNull(file);
    }

    @Test
    void generatorAndSaveStream() {
        Flux<String> codeStream = aiCodeGeneratorFacade.GeneratorAndSaveStream
                ("生成一个个人主页页面，要求代码在20行内", CodeGenTypeEnum.HTML, 1L);
        List<String> result = codeStream.collectList().block();
        Assertions.assertNotNull(result);
        String completeContent = String.join("", result);
        Assertions.assertNotNull(completeContent);
    }

    @Test
    void generatorVueProjectCodeStream(){
        Flux<String> codeStream = aiCodeGeneratorFacade.GeneratorAndSaveStream
                ("生成一个日程表网站，要求代码在100行内，要以最少的代码量生成，最简单的就行", CodeGenTypeEnum.VUE_PROJECT, 2L);
        List<String> result = codeStream.collectList().block();
        Assertions.assertNotNull(result);
        String completeContent = String.join("", result);
        Assertions.assertNotNull(completeContent);
    }
}