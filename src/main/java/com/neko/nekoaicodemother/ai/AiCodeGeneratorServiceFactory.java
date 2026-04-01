package com.neko.nekoaicodemother.ai;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.neko.nekoaicodemother.service.ChatHistoryService;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@Slf4j
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
     * 聊天记录服务
     */
    @Resource
    private ChatHistoryService chatHistoryService;

    /**
     * 集成 Redis 对话记忆库
     */
    @Resource
    private RedisChatMemoryStore redisChatMemoryStore;

    /**
     * 使用 Caffeine 本地缓存来缓存 AiService 避免同个用户重复创建 AiService
     */
    private final Cache<Long, AiCodeGeneratorService> serviceCache = Caffeine.newBuilder()
            .maximumSize(1000) // 最大缓存实例
            .expireAfterWrite(Duration.ofMinutes(30)) // 初次写入缓存设置 30分钟过期
            .expireAfterAccess(Duration.ofMinutes(10)) // 再次命中缓存后 10分钟过期
            .removalListener((key, value, cause) -> log.debug("缓存被删除: appId={}, cause={}", key, cause))
            .build();

    /**
     * 创建 AiCodeGeneratorService
     * @param appId 应用 ID
     * @return AiCodeGeneratorService
     */
    public AiCodeGeneratorService createAiCodeGeneratorService(long appId) {
        log.info("创建 AiCodeGeneratorService, appId={}", appId);
        // 根据 appId 初始化不同的对话记忆库
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder()
                .id(appId)
                .chatMemoryStore(redisChatMemoryStore)
                .maxMessages(20)
                .build();
        // 初始化对话记忆库(缓存)
        chatHistoryService.loadChatHistoryToMemory(appId, chatMemory, 20);
        // 根据对话记忆库来创建不同的 AIService
        return AiServices.builder(AiCodeGeneratorService.class)
                .chatModel(chatModel)
                .streamingChatModel(streamingChatModel)
                .chatMemory(chatMemory)
                .build();
    }

    /**
     * 从缓存中获取 AiCodeGeneratorService，没有就创建
     * @param appId 应用 ID
     * @return AiCodeGeneratorService
     */
    public AiCodeGeneratorService getAiCodeGeneratorService(long appId) {
        // 先从缓存中获取 AiService，如果缓存未命中，则创建一个新的 AiService
        return serviceCache.get(appId, this::createAiCodeGeneratorService);
    }

    /**
     * 创建 AiCodeGeneratorService 的工厂类
     */
    @Bean
    public AiCodeGeneratorService aiCodeGeneratorService() {
        // 提供默认使用
        return this.getAiCodeGeneratorService(0L);
    }
}
