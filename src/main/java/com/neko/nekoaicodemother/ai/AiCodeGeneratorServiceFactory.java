package com.neko.nekoaicodemother.ai;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiCodeGeneratorServiceFactory {

    /**
     * 聊天模型
     */
    @Resource
    private ChatModel chatModel;

    /**
     * SSE流式聊天模型
     */
    @Resource
    private StreamingChatModel streamingChatModel;
    /**
     * 创建 AiCodeGeneratorService 的工厂类
     */
    @Bean
    public AiCodeGeneratorService aiCodeGeneratorService() {
        // 为 AiCodeGeneratorService 创建 AiServices 对象
        return AiServices.builder(AiCodeGeneratorService.class)
                .chatModel(chatModel)
                .streamingChatModel(streamingChatModel)
                .build();
    }
}
