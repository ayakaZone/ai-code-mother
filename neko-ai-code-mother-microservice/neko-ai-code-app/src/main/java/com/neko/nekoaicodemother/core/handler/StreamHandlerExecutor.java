package com.neko.nekoaicodemother.core.handler;

import com.neko.nekoaicodemother.model.entity.User;
import com.neko.nekoaicodemother.model.enums.CodeGenTypeEnum;
import com.neko.nekoaicodemother.service.ChatHistoryService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Slf4j
@Component
public class StreamHandlerExecutor {

    @Resource
    private JsonMessageStreamHandler jsonMessageStreamHandler;

    /**
     * 处理流式响应的执行器，保存对话历史并返回给前端
     * @param originFlux 响应流
     * @param chatHistoryService 对话历史服务
     * @param appId 应用 ID
     * @param loginUser 登录用户
     * @param codeGenTypeEnum 代码生成类型
     * @return 处理后的流式响应
     */
    public Flux<String> doExecutor(Flux<String> originFlux, ChatHistoryService chatHistoryService,
                                   Long appId, User loginUser, CodeGenTypeEnum codeGenTypeEnum) {
        return switch (codeGenTypeEnum) {
            case VUE_PROJECT -> jsonMessageStreamHandler.handle(originFlux, chatHistoryService, appId, loginUser);
            case HTML, MULTI_FILE -> new SimpleTextStreamHandler().handle(originFlux, chatHistoryService, appId, loginUser);
        };
    }
}
