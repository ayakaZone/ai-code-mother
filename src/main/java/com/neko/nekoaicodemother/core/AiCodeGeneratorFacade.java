package com.neko.nekoaicodemother.core;

import cn.hutool.json.JSONUtil;
import com.neko.nekoaicodemother.ai.AiCodeGeneratorService;
import com.neko.nekoaicodemother.ai.AiCodeGeneratorServiceFactory;
import com.neko.nekoaicodemother.ai.model.HtmlCodeResult;
import com.neko.nekoaicodemother.ai.model.MultiFileCodeResult;
import com.neko.nekoaicodemother.ai.model.message.AiResponseMessage;
import com.neko.nekoaicodemother.ai.model.message.ToolExecutedMessage;
import com.neko.nekoaicodemother.ai.model.message.ToolRequestMessage;
import com.neko.nekoaicodemother.exception.BusinessException;
import com.neko.nekoaicodemother.exception.ErrorCode;
import com.neko.nekoaicodemother.model.enums.CodeGenTypeEnum;
import com.neko.nekoaicodemother.core.parser.CodeParserExecutor;
import com.neko.nekoaicodemother.core.saver.CodeFileSaverExecutor;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.ToolExecution;
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
    private AiCodeGeneratorServiceFactory aiCodeGeneratorServiceFactory;

    /**
     * AI 代码生成保存文件门面类
     *
     * @param userMessage     用户消息
     * @param codeGenTypeEnum 代码生成类型枚举
     * @return 文件
     */
    public File GeneratorAndSave(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "代码生成类型错误");
        }
        // 通过 appId 在 AiService 工厂中获取 AiService
        AiCodeGeneratorService aiCodeGeneratorService = aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(appId, codeGenTypeEnum);
        return switch (codeGenTypeEnum) {
            case HTML -> {
                HtmlCodeResult htmlCodeResult = aiCodeGeneratorService.generateHtmlCode(userMessage);
                yield CodeFileSaverExecutor.executeSaver(htmlCodeResult, CodeGenTypeEnum.HTML, appId);
            }
            case MULTI_FILE -> {
                MultiFileCodeResult multiFileCodeResult = aiCodeGeneratorService.generateMultiFileCode(userMessage);
                yield CodeFileSaverExecutor.executeSaver(multiFileCodeResult, CodeGenTypeEnum.MULTI_FILE, appId);
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
    public Flux<String> GeneratorAndSaveStream(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "代码生成类型错误");
        }
        // 通过 appId 在 AiService 工厂中获取 AiService
        AiCodeGeneratorService aiCodeGeneratorService = aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(appId, codeGenTypeEnum);
        return switch (codeGenTypeEnum) {
            case HTML -> {
                Flux<String> codeStream = aiCodeGeneratorService.generatorHtmlCodeStream(userMessage);
                yield processCodeStream(codeStream, CodeGenTypeEnum.HTML, appId);
            }
            case MULTI_FILE -> {
                Flux<String> codeStream = aiCodeGeneratorService.generatorMultiFileCodeStream(userMessage);
                yield processCodeStream(codeStream, CodeGenTypeEnum.MULTI_FILE, appId);
            }
            case VUE_PROJECT -> {
                TokenStream tokenStream = aiCodeGeneratorService.generatorVueProjectCodeStream(appId, userMessage);
                yield processTokenStream(tokenStream);
            }
            default -> {
                String errorMessage = "不支持的生成类型：" + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };
    }

    /**
     * 将 TokenStream 流处理成 Flux 流
     * @param tokenStream TokenStream
     * @return Flux
     */
    private Flux<String> processTokenStream(TokenStream tokenStream) {
        return Flux.create(sink -> {
            // 处理 AI 响应的消息
            tokenStream.onPartialResponse((String partialResponse) -> {
                        AiResponseMessage aiResponseMessage = new AiResponseMessage(partialResponse);
                        sink.next(JSONUtil.toJsonStr(aiResponseMessage));
                    })
                    // 处理 AI 工具调用请求的消息
                    .onPartialToolExecutionRequest((index, toolExecutionRequest) -> {
                        ToolRequestMessage toolRequestMessage = new ToolRequestMessage(toolExecutionRequest);
                        sink.next(JSONUtil.toJsonStr(toolRequestMessage));
                    })
                    // 处理 AI 工具调用执行结果的消息
                    .onToolExecuted((ToolExecution toolExecution) -> {
                        ToolExecutedMessage toolExecutedMessage = new ToolExecutedMessage(toolExecution);
                        sink.next(JSONUtil.toJsonStr(toolExecutedMessage));
                    })
                    // 通知流式响应正常完成
                    .onCompleteResponse((ChatResponse chatResponse) -> {
                        sink.complete();
                    })
                    // 通知流式响应出现异常
                    .onError((Throwable error) -> {
                        // 打印错误堆栈
                        error.printStackTrace();
                        sink.error(error);
                    }).start();
        });
    }



    /**
     * 处理代码流并保存
     *
     * @param codeStream      代码流
     * @param codeGenTypeEnum 代码生成类型枚举
     * @return Flux
     */
    private Flux<String> processCodeStream(Flux<String> codeStream, CodeGenTypeEnum codeGenTypeEnum, Long appId) {
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
                        File savedDir = CodeFileSaverExecutor.executeSaver(parseResult, codeGenTypeEnum, appId);
                        log.info("代码保存成功,保存路径：{}", savedDir.getAbsolutePath());
                    } catch (Exception e) {
                        log.error("代码保存失败", e);
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
