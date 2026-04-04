package com.neko.nekoaicodemother.core.handler;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.neko.nekoaicodemother.ai.model.message.*;
import com.neko.nekoaicodemother.ai.tools.BaseTool;
import com.neko.nekoaicodemother.ai.tools.ToolManager;
import com.neko.nekoaicodemother.constant.AppConstant;
import com.neko.nekoaicodemother.core.builder.VueProjectBuilder;
import com.neko.nekoaicodemother.model.entity.User;
import com.neko.nekoaicodemother.model.enums.ChatHistoryMessageTypeEnum;
import com.neko.nekoaicodemother.model.enums.CodeGenTypeEnum;
import com.neko.nekoaicodemother.service.ChatHistoryService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * AI JSON 消息流处理器
 * 处理 Vue_Project 类型的流式响应结果，包含工具调用信息
 * 将工具调用信息返回给前端展示给用户
 * 再保存到对话历史数据库中
 */
@Slf4j
@Component
public class JsonMessageStreamHandler {

    @Resource
    private VueProjectBuilder vueProjectBuilder;

    @Resource
    private ToolManager toolManager;

    /**
     * 处理 JSON 响应流
     * 返回给前端的消息
     * 保存到后端对话历史数据库的消息
     *
     * @param originFlux         响应流
     * @param chatHistoryService 对话历史服务
     * @param appId              应用 ID
     * @param loginUser          登录用户
     * @return Flux
     */
    public Flux<String> handle(Flux<String> originFlux, ChatHistoryService chatHistoryService, Long appId, User loginUser) {
        // 处理文本
        StringBuilder stringBuilder = new StringBuilder();
        // 保存 AI 调用工具的工具 ID
        Set<String> seenToolIds = new HashSet<>();
        return originFlux.map(chunk -> {
                    // 解析 JSON 消息
                    return handleJsonMessageChunk(chunk, stringBuilder, seenToolIds);
                })
                .filter(StrUtil::isNotBlank)
                .doOnComplete(() -> {
                    // 流式响应完成后，添加 AI 消息到对话历史
                    String aiResponse = stringBuilder.toString();
                    chatHistoryService.addChatHistory(appId, aiResponse, ChatHistoryMessageTypeEnum.AI.getValue(), loginUser.getId());
                    // 获取构建项目路径并构建项目
                    String projectPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + CodeGenTypeEnum.VUE_PROJECT.getValue() + "_" + appId;
                    vueProjectBuilder.buildProjectAsync(projectPath);
                }).doOnError(error -> {
                    // 如果 AI 回复异常，记录错误消息
                    String errorMessage = "AI 回复失败：" + error.getMessage();
                    chatHistoryService.addChatHistory(appId, errorMessage, ChatHistoryMessageTypeEnum.AI.getValue(), loginUser.getId());
                });
    }

    /**
     * 处理 JSON 消息
     *
     * @param chunk         消息流
     * @param stringBuilder 拼接文本
     * @param seenToolIds   已调用的工具 ID
     * @return 处理后的文本
     */
    private String handleJsonMessageChunk(String chunk, StringBuilder stringBuilder, Set<String> seenToolIds) {
        // 将 Json 转为消息类
        StreamMessage streamMessage = JSONUtil.toBean(chunk, StreamMessage.class);
        // 获得 AI 的消息类型
        StreamMessageTypeEnum messageTypeEnum = StreamMessageTypeEnum.getEnumByValue(streamMessage.getType());
        // 根据不同的消息类型处理 JSON 数据
        switch (messageTypeEnum) {
            case StreamMessageTypeEnum.AI_RESPONSE -> {
                // JSON 转消息响应类
                AiResponseMessage aiResponseMessage = JSONUtil.toBean(chunk, AiResponseMessage.class);
                // 拼接响应结果
                String data = aiResponseMessage.getData();
                stringBuilder.append(data);
                return data;
            }
            case StreamMessageTypeEnum.TOOL_REQUEST -> {
                // JSON 转工具请求响应类
                ToolRequestMessage toolRequestMessage = JSONUtil.toBean(chunk, ToolRequestMessage.class);
                // 获取调用工具Id和Name
                String toolId = toolRequestMessage.getId();
                String toolName = toolRequestMessage.getName();
                // 如果工具是第一次调用
                if (toolId != null && !seenToolIds.contains(toolId)) {
                    // 记录工具 id
                    seenToolIds.add(toolId);
                    // 获取调用的工具实例
                    BaseTool tool = toolManager.getTool(toolName);
                    // 返回工具调用信息
                    return tool.generateToolRequestResponse();
                } else {
                    // 重复调用工具，不处理
                    return "";
                }
            }
            case StreamMessageTypeEnum.TOOL_EXECUTED -> {
                // JSON 转工具执行结果类
                ToolExecutedMessage toolExecutedMessage = JSONUtil.toBean(chunk, ToolExecutedMessage.class);
                // 将调用工具的参数转为 JSON 对象
                JSONObject jsonObject = JSONUtil.parseObj(toolExecutedMessage.getArguments());
                // 根据调用工具名称后去工具实例
                BaseTool tool = toolManager.getTool(toolExecutedMessage.getName());
                // 返回调用工具的结果
                String result = tool.generateToolExecuteResponse(jsonObject);
                // 输出前端和要持久化的文件
                String output = String.format("\n\n%s\n\n", result);
                stringBuilder.append(output);
                return output;
            }
            default -> {
                log.error("不支持的流式消息类型：{}", messageTypeEnum);
                return "";
            }
        }
    }
}
