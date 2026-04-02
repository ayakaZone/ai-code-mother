package com.neko.nekoaicodemother.config;


import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 推理模型配置类
 */
@Configuration
@ConfigurationProperties(prefix = "langchain4j.open-ai.chat-model")
@Data
public class ReasoningStreamingChatModelConfig {

    private String baseUrl;

    private String apiKey;

    /**
     * 创建推理模型
     * @return StreamingChatModel
     */
    @Bean
    public StreamingChatModel reasoningStreamingChatModel() {
        // AI 普通模型
        final String modelName = "deepseek-chat";
        final int maxToken = 8192;
        // AI 推理模型(生产环境使用)
//        final String modelName = "deepseek-reasoner";
//        final int maxToken = 32768;
        return OpenAiStreamingChatModel.builder()
                .modelName(modelName)
                .maxTokens(maxToken)
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .logRequests(true)
                .logResponses(true)
                .build();
    }
}
