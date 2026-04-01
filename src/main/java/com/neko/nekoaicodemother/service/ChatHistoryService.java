package com.neko.nekoaicodemother.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.neko.nekoaicodemother.model.dto.chathistory.ChatHistoryQueryRequest;
import com.neko.nekoaicodemother.model.entity.ChatHistory;
import com.neko.nekoaicodemother.model.entity.User;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

import java.time.LocalDateTime;

/**
 * 对话历史 服务层。
 *
 * @author mosoNeko
 */
public interface ChatHistoryService extends IService<ChatHistory> {

    /**
     * 保存对话历史。
     *
     * @param userId      用户id
     * @param message     消息
     * @param messageType 消息类型
     * @param appId       应用id
     * @return 保存成功
     */
    boolean addChatHistory(Long appId, String message, String messageType, Long userId);

    /**
     * 删除对话历史
     *
     * @param appId 应用id
     * @return 删除成功
     */
    boolean deleteChatHistory(Long appId);

    /**
     * 获取应用对话历史
     * @param appId 应用Id
     * @param pageSize 页大小
     * @param lastCreateTime 上次创建时间
     * @param loginUser 登录用户
     * @return 应用对话历史
     */
    Page<ChatHistory> listAppChatHistoryByPage(Long appId, int pageSize, LocalDateTime lastCreateTime, User loginUser);

    /**
     * 获取查询包装类
     * @param chatHistoryQueryRequest 查询条件
     * @return 查询包装类
     */
    QueryWrapper getQueryWrapper(ChatHistoryQueryRequest chatHistoryQueryRequest);

    /**
     * 加载对话历史到内存
     * @param appId 应用Id
     * @param memory 内存
     * @param maxCount 最大数量
     * @return 加载数量
     */
    int loadChatHistoryToMemory(Long appId, MessageWindowChatMemory memory, int maxCount);
}
