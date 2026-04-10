package com.neko.nekoaicodemother.ai;

import com.neko.nekoaicodemother.ai.model.HtmlCodeResult;
import com.neko.nekoaicodemother.ai.model.MultiFileCodeResult;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import reactor.core.publisher.Flux;

/**
 * AI 代码生成聊天模型接口
 */
public interface AiCodeGeneratorService {

    /**
     * 生成 HTML 代码
     * @param userMessage 用户消息
     * @return HTML 代码实体类
     */
    @SystemMessage(fromResource = "prompt/codegen-html-system-prompt.txt")
    HtmlCodeResult generateHtmlCode(String userMessage);

    /**
     * 生成多文件代码
     * @param userMessage 用户消息
     * @return 多文件代码实体类
     */
    @SystemMessage(fromResource = "prompt/codegen-multi-file-system-prompt.txt")
    MultiFileCodeResult generateMultiFileCode(String userMessage);

    /**
     * 生成 HTML 代码流
     * @param userMessage 用户消息
     * @return HTML 代码流
     */
    @SystemMessage(fromResource = "prompt/codegen-html-system-prompt.txt")
    Flux<String> generatorHtmlCodeStream(String userMessage);

    /**
     * 生成多文件代码流
     * @param userMessage 用户消息
     * @return 多文件代码流
     */
    @SystemMessage(fromResource = "prompt/codegen-multi-file-system-prompt.txt")
    Flux<String> generatorMultiFileCodeStream(String userMessage);

    /**
     * 生成 Vue 项目代码流
     * @param appId 应用 ID
     * @param userMessage 用户消息
     * @return Vue 项目代码流
     */
    @SystemMessage(fromResource = "prompt/codegen-vue-project-system-prompt.txt")
    TokenStream generatorVueProjectCodeStream(@MemoryId Long appId, @UserMessage String userMessage);
}
