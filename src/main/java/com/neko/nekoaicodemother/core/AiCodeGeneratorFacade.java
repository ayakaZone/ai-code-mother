package com.neko.nekoaicodemother.core;

import com.neko.nekoaicodemother.ai.AiCodeGeneratorService;
import com.neko.nekoaicodemother.ai.model.HtmlCodeResult;
import com.neko.nekoaicodemother.ai.model.MultiFileCodeResult;
import com.neko.nekoaicodemother.exception.BusinessException;
import com.neko.nekoaicodemother.exception.ErrorCode;
import com.neko.nekoaicodemother.model.enums.CodeGenTypeEnum;
import com.neko.nekoaicodemother.parser.CodeParserExecutor;
import com.neko.nekoaicodemother.saver.CodeFileSaverExecutor;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;

/**
 * AI 代码生成门面模式类
 */
@Service
@Slf4j
public class AiCodeGeneratorFacade {

    @Resource
    private AiCodeGeneratorService aiCodeGeneratorService;

    /**
     * AI 代码生成保存文件门面类
     *
     * @param userMessage     用户消息
     * @param codeGenTypeEnum 代码生成类型枚举
     * @return 文件
     */
    public File GeneratorAndSave(String userMessage, CodeGenTypeEnum codeGenTypeEnum) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "代码生成类型错误");
        }
        return switch (codeGenTypeEnum) {
            case HTML -> {
                HtmlCodeResult htmlCodeResult = aiCodeGeneratorService.generateHtmlCode(userMessage);
                yield CodeFileSaverExecutor.executeSaver(htmlCodeResult, CodeGenTypeEnum.HTML);
            }
            case MULTI_FILE -> {
                MultiFileCodeResult multiFileCodeResult = aiCodeGeneratorService.generateMultiFileCode(userMessage);
                yield CodeFileSaverExecutor.executeSaver(multiFileCodeResult, CodeGenTypeEnum.MULTI_FILE);
            }
            default -> {
                String errorMessage = "不支持的生成类型：" + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };
    }

    /**
     * AI 代码生成保存文件流门面类
     *
     * @param userMessage     用户消息
     * @param codeGenTypeEnum 代码生成类型枚举
     * @return Flux
     *
     */
    public Flux<String> GeneratorAndSaveStream(String userMessage, CodeGenTypeEnum codeGenTypeEnum) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "代码生成类型错误");
        }
        return switch (codeGenTypeEnum) {
            case HTML -> {
                Flux<String> result = aiCodeGeneratorService.generatorHtmlCodeStream(userMessage);
                yield processCodeStream(result, CodeGenTypeEnum.HTML);
            }
            case MULTI_FILE -> {
                Flux<String> result = aiCodeGeneratorService.generatorMultiFileCodeStream(userMessage);
                yield processCodeStream(result, CodeGenTypeEnum.MULTI_FILE);
            }
            default -> {
                String errorMessage = "不支持的生成类型：" + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };
    }

    /**
     * 处理代码流并保存
     *
     * @param codeStream      代码流
     * @param codeGenTypeEnum 代码生成类型枚举
     * @return Flux
     */
    private Flux<String> processCodeStream(Flux<String> codeStream, CodeGenTypeEnum codeGenTypeEnum) {
        // 使用 StringBuilder 拼接输出内容
        StringBuilder codeBuilder = new StringBuilder();
        // chunk 流的输出
        return codeStream.doOnNext(codeBuilder::append)
                // 输出完成
                .doOnComplete(() -> {
                    try {
                        // 解析HTML代码转为对象
                        String completeCode = codeBuilder.toString();
                        Object parseResult = CodeParserExecutor.ExecutorParser(completeCode, codeGenTypeEnum);
                        // 写入文件
                        File savedDir = CodeFileSaverExecutor.executeSaver(parseResult, codeGenTypeEnum);
                        log.info("HTML代码保存成功,保存路径：{}", savedDir.getAbsolutePath());
                    } catch (Exception e) {
                        log.error("HTML代码保存失败", e);
                    }
                });
    }

//    /**
//     * 生成HTML代码并保存
//     * @param userMessage 用户消息
//     * @return 文件
//     */
//    private File generatorAndSaveHtmlCode(String userMessage) {
//        HtmlCodeResult htmlCodeResult = aiCodeGeneratorService.generateHtmlCode(userMessage);
//        return CodeFileSaverExecutor.executeSaver(htmlCodeResult, CodeGenTypeEnum.HTML);
//    }
//
//    /**
//     * 生成多文件代码并保存
//     * @param userMessage 用户消息
//     * @return 文件
//     */
//    private File generatorAndSaveMultiFileCode(String userMessage) {
//        MultiFileCodeResult multiFileCodeResult = aiCodeGeneratorService.generateMultiFileCode(userMessage);
//        return CodeFileSaverExecutor.executeSaver(multiFileCodeResult, CodeGenTypeEnum.MULTI_FILE);
//    }
//
//    /**
//     * 生成HTML代码并保存流
//     * @param userMessage 用户消息
//     * @return Flux
//     */
//    private Flux<String> generatorAndSaveHtmlCodeStream(String userMessage) {
//        // 得到流
//        Flux<String> result = aiCodeGeneratorService.generatorHtmlCodeStream(userMessage);
//        return processCodeStream(result, CodeGenTypeEnum.HTML);
//    }
//
//    /**
//     * 生成多文件代码并保存流
//     * @param userMessage 用户消息
//     * @return Flux
//     */
//    private Flux<String> generatorAndSaveMultiFileCodeStream(String userMessage) {
//        Flux<String> result = aiCodeGeneratorService.generatorMultiFileCodeStream(userMessage);
//        return processCodeStream(result, CodeGenTypeEnum.MULTI_FILE);
//    }
}
