package com.neko.nekoaicodemother.core.handler;

import cn.hutool.core.util.StrUtil;
import com.neko.nekoaicodemother.model.entity.User;
import com.neko.nekoaicodemother.model.enums.ChatHistoryMessageTypeEnum;
import com.neko.nekoaicodemother.service.ChatHistoryService;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

/**
 * AI 原生文本流处理器
 * 处理 HTML 和 Multi_File 类型的流式响应结果
 * 再保存到对话历史数据库中
 */
@Slf4j
public class SimpleTextStreamHandler {

    public Flux<String> handle(Flux<String> originFlux, ChatHistoryService chatHistoryService, Long appId, User loginUser) {
        StringBuilder aiResponseBuilder = new StringBuilder();
        return originFlux.map(chunk -> {
            // 收集并拼接流
            aiResponseBuilder.append(chunk);
            return chunk;
        }).doOnComplete(() -> {
            // 流输出完成后，保存对话历史
            String aiResponse = aiResponseBuilder.toString();
            if (StrUtil.isNotBlank(aiResponse)) {
                chatHistoryService.addChatHistory
                        (appId, aiResponseBuilder.toString(), ChatHistoryMessageTypeEnum.AI.getValue(), loginUser.getId());
            }
        }).doOnError(error -> {
            // ai输出发生错误也要保存错误信息到对话历史
            String errorMessage = "AI 回复失败：" + error.getMessage();
            chatHistoryService.addChatHistory(appId, errorMessage, ChatHistoryMessageTypeEnum.AI.getValue(), loginUser.getId());
        });
    }
}
