package com.neko.nekoaicodemother.ai;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.neko.nekoaicodemother.ai.guardrail.PromptSafetyInputGuardrail;
import com.neko.nekoaicodemother.ai.tools.*;
import com.neko.nekoaicodemother.exception.BusinessException;
import com.neko.nekoaicodemother.exception.ErrorCode;
import com.neko.nekoaicodemother.model.enums.CodeGenTypeEnum;
import com.neko.nekoaicodemother.service.ChatHistoryService;
import com.neko.nekoaicodemother.utils.SpringContextUtil;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
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
    @Resource(name = "openAiChatModel")
    private ChatModel chatModel;

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
     * 工具管理
     */
    @Resource
    private ToolManager toolManager;

    /**
     * 使用 Caffeine 本地缓存来缓存 AiService 避免同个用户重复创建 AiService
     */
    private final Cache<String, AiCodeGeneratorService> serviceCache = Caffeine.newBuilder()
            .maximumSize(1000) // 最大缓存实例
            .expireAfterWrite(Duration.ofMinutes(30)) // 初次写入缓存设置 30分钟过期
            .expireAfterAccess(Duration.ofMinutes(10)) // 再次命中缓存后 10分钟过期
            .removalListener((key, value, cause) -> log.debug("缓存被删除: appId={}, cause={}", key, cause))
            .build();

    /**
     * 创建 AiCodeGeneratorService
     *
     * @param appId 应用 ID
     * @return AiCodeGeneratorService
     */
    public AiCodeGeneratorService createAiCodeGeneratorService(long appId, CodeGenTypeEnum codeGenTypeEnum) {
        log.info("创建 AiCodeGeneratorService, appId={}", appId);
        // 根据 appId 初始化不同的对话记忆库
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder()
                .id(appId)
                .chatMemoryStore(redisChatMemoryStore)
                .maxMessages(50)
                .build();
        // 初始化对话记忆库(缓存)
        chatHistoryService.loadChatHistoryToMemory(appId, chatMemory, 50);
        // 根据不同的代码类型创建 AIService
        return switch (codeGenTypeEnum) {
            // 非 Vue 工程不需要提供工具调用和 Provider
            case CodeGenTypeEnum.HTML, CodeGenTypeEnum.MULTI_FILE -> {
                // 使用多例模式的 StreamingChatModel 解决并发问题
                StreamingChatModel openAiStreamingChatModel =
                        SpringContextUtil.getBean("streamingChatModelPrototype", StreamingChatModel.class);
                yield AiServices.builder(AiCodeGeneratorService.class)
                        .chatModel(chatModel)
                        .streamingChatModel(openAiStreamingChatModel)
                        .chatMemory(chatMemory)
                        // 提示词护轨
                        .inputGuardrails(new PromptSafetyInputGuardrail())
                        // AI 输出护轨 与 AI 流式输出有冲突
                        //  .outputGuardrails(new RetryOutputGuardrail())
                        // 工具调用最大次数
                        .maxSequentialToolsInvocations(20)
                        .build();
            }
            case CodeGenTypeEnum.VUE_PROJECT -> {
                // 使用多例模式的 StreamingChatModel 解决并发问题
                StreamingChatModel reasoningStreamingChatModel =
                        SpringContextUtil.getBean("reasoningStreamingChatModelPrototype", StreamingChatModel.class);
                yield AiServices.builder(AiCodeGeneratorService.class)
                        .streamingChatModel(reasoningStreamingChatModel)
                        // 对话记忆（使用工具调用必须要用 Provider）
                        .chatMemoryProvider(memoryId -> chatMemory)
                        // 提供的工具
                        .tools(toolManager.getAllTools())
                        // 防止 AI 调用不存在的工具
                        .hallucinatedToolNameStrategy(
                                toolExecutionRequest -> ToolExecutionResultMessage.from(
                                        toolExecutionRequest, "Error: there is no tool called " + toolExecutionRequest.name()))
                        // 提示词护轨
                        .inputGuardrails(new PromptSafetyInputGuardrail())
                        // AI 输出护轨 与 AI 流式输出有冲突
                        // .outputGuardrails(new RetryOutputGuardrail())
                        // 工具调用最大次数
                        .maxSequentialToolsInvocations(20)
                        .build();
            }
            default ->
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "不支持的代码生成类型：" + codeGenTypeEnum.getValue());
        };
    }

    /**
     * 获取 AiCodeGeneratorService(提供默认调用)
     *
     * @param appId 应用 ID
     * @return AiCodeGeneratorService
     */
    public AiCodeGeneratorService getAiCodeGeneratorService(long appId) {
        // 先从缓存中获取 AiService，如果缓存未命中，则创建一个新的 AiService
        return getAiCodeGeneratorService(appId, CodeGenTypeEnum.HTML);
    }

    /**
     * 从缓存中获取 AiCodeGeneratorService，没有就创建
     *
     * @param appId 应用 ID
     * @return AiCodeGeneratorService
     */
    public AiCodeGeneratorService getAiCodeGeneratorService(long appId, CodeGenTypeEnum codeGenTypeEnum) {
        String cacheKey = buildCacheKey(appId, codeGenTypeEnum);
        // 先从缓存中获取 AiService，如果缓存未命中，则创建一个新的 AiService
        return serviceCache.get(cacheKey, key -> createAiCodeGeneratorService(appId, codeGenTypeEnum));
    }

    /**
     * 构建缓存的 key
     *
     * @param appId           应用 ID
     * @param codeGenTypeEnum 代码生成类型
     * @return 缓存的 key
     */
    private String buildCacheKey(long appId, CodeGenTypeEnum codeGenTypeEnum) {
        return appId + "_" + codeGenTypeEnum.getValue();
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
