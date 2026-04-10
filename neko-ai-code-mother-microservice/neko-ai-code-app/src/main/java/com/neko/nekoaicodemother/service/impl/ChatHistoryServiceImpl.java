package com.neko.nekoaicodemother.service.impl;

import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.neko.nekoaicodemother.exception.ErrorCode;
import com.neko.nekoaicodemother.exception.ThrowUtils;
import com.neko.nekoaicodemother.mapper.ChatHistoryMapper;
import com.neko.nekoaicodemother.model.dto.chathistory.ChatHistoryQueryRequest;
import com.neko.nekoaicodemother.model.entity.App;
import com.neko.nekoaicodemother.model.entity.ChatHistory;
import com.neko.nekoaicodemother.model.entity.User;
import com.neko.nekoaicodemother.model.enums.ChatHistoryMessageTypeEnum;
import com.neko.nekoaicodemother.model.enums.UserRoleEnum;
import com.neko.nekoaicodemother.service.AppService;
import com.neko.nekoaicodemother.service.ChatHistoryService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 对话历史 服务层实现。
 *
 * @author mosoNeko
 */
@Service
@Slf4j
public class ChatHistoryServiceImpl extends ServiceImpl<ChatHistoryMapper, ChatHistory> implements ChatHistoryService {

    @Resource
    @Lazy
    private AppService appService;


    /**
     * 添加对话历史
     *
     * @param appId       应用Id
     * @param message     消息
     * @param messageType 消息类型
     * @param userId      用户Id
     * @return 是否添加成功
     */
    @Override
    public boolean addChatHistory(Long appId, String message, String messageType, Long userId) {
        // 校验参数
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR, "用户Id不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "消息不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(messageType), ErrorCode.PARAMS_ERROR, "消息类型不能为空");
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用Id不能为空");
        // 校验消息类型
        ChatHistoryMessageTypeEnum messageTypeEnum = ChatHistoryMessageTypeEnum.getEnumByValue(messageType);
        ThrowUtils.throwIf(messageTypeEnum == null, ErrorCode.PARAMS_ERROR, "消息类型错误");
        // 构造对象
        ChatHistory chatHistory = ChatHistory.builder().appId(appId).message(message).messageType(messageType).userId(userId).build();
        // 插入数据库
        return this.save(chatHistory);
    }

    /**
     * 删除对话历史
     *
     * @param appId 应用Id
     * @return 是否删除成功
     */
    @Override
    public boolean deleteChatHistory(Long appId) {
        // 校验参数
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "id不能为空");
        // 构造删除条件
        QueryWrapper queryWrapper = QueryWrapper.create().eq(ChatHistory::getAppId, appId);
        return this.remove(queryWrapper);
    }

    /**
     * 获取应用对话历史
     * @param appId 应用Id
     * @param pageSize 页大小
     * @param lastCreateTime 上次创建时间
     * @param loginUser 登录用户
     * @return 应用对话历史
     */
    @Override
    public Page<ChatHistory> listAppChatHistoryByPage(Long appId, int pageSize, LocalDateTime lastCreateTime, User loginUser) {
        // 校验参数
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用Id不能为空");
        ThrowUtils.throwIf(pageSize <= 0 || pageSize > 50, ErrorCode.PARAMS_ERROR, "页面大小必须在1-50之间");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        // 判断应用是否存在
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 判断是否有查询权限（仅用户本人或管理员）
        boolean isAdmin = loginUser.getUserRole().equals(UserRoleEnum.ADMIN.getValue());
        boolean isCreator = loginUser.getId().equals(app.getUserId());
        ThrowUtils.throwIf(!isAdmin && !isCreator, ErrorCode.NO_AUTH_ERROR);
        // 构造查询条件
        ChatHistoryQueryRequest queryRequest = new ChatHistoryQueryRequest();
        queryRequest.setAppId(appId);
        queryRequest.setLastCreateTime(lastCreateTime);
        QueryWrapper queryWrapper = this.getQueryWrapper(queryRequest);
        return this.page(Page.of(1, pageSize), queryWrapper);
    }

    /**
     * 获取查询条件
     * @param chatHistoryQueryRequest 查询条件
     * @return 查询条件
     */
    @Override
    public QueryWrapper getQueryWrapper(ChatHistoryQueryRequest chatHistoryQueryRequest) {
        // 校验参数
        QueryWrapper queryWrapper = QueryWrapper.create();
        if (chatHistoryQueryRequest == null) {
            return queryWrapper;
        }
        Long id = chatHistoryQueryRequest.getId();
        String message = chatHistoryQueryRequest.getMessage();
        String messageType = chatHistoryQueryRequest.getMessageType();
        Long appId = chatHistoryQueryRequest.getAppId();
        Long userId = chatHistoryQueryRequest.getUserId();
        LocalDateTime lastCreateTime = chatHistoryQueryRequest.getLastCreateTime();
        String sortField = chatHistoryQueryRequest.getSortField();
        String sortOrder = chatHistoryQueryRequest.getSortOrder();
        // 构造查询条件
        queryWrapper.eq(ChatHistory::getId, id)
                .like(ChatHistory::getMessage, message)
                .eq(ChatHistory::getMessageType, messageType)
                .eq(ChatHistory::getAppId, appId)
                .eq(ChatHistory::getUserId, userId);
        // 判断是否需要游标查询
        if (lastCreateTime != null) {
            queryWrapper.lt(ChatHistory::getCreateTime, lastCreateTime);
        }
        // 判断是否有排序字段、方式
        if (sortField != null) {
            queryWrapper.orderBy(sortField, "ascend".equals(sortOrder));
        } else {
            // 默认按创建时间降序排序
            queryWrapper.orderBy(ChatHistory::getCreateTime, false);
        }
        return queryWrapper;
    }

    /**
     * 加载对话历史到内存（对话记忆库初始化，防止缓存过期）
     * @param appId 应用Id
     * @param memory 内存
     * @param maxCount 最大数量
     * @return 加载数量
     */
    @Override
    public int loadChatHistoryToMemory(Long appId, MessageWindowChatMemory memory, int maxCount) {
        try {
            // 构造查询条件
            QueryWrapper queryWrapper = QueryWrapper.create()
                    .eq(ChatHistory::getAppId, appId)
                    .orderBy(ChatHistory::getCreateTime, false)
                    .limit(1, maxCount);
            // 从数据库中查询匹配的对话历史记录
            List<ChatHistory> chatHistoryList = this.list(queryWrapper);
            // 没有对话记录直接返回，无需初始化记忆
            if (chatHistoryList == null || chatHistoryList.isEmpty()) {
                return 0;
            }
            // 查询数据是降序排序，新的在上，旧的在下，聊天信息类的数据应该是旧的在上，新的在下，所以需要反转列表
//            chatHistoryList = chatHistoryList.reversed();
            // 添加到对话历史记忆库中,需要先清空记忆库，避免重复加载对话记忆
            memory.clear();
            int loadCount = 0;
            for (ChatHistory chatHistory : chatHistoryList) {
                // 判断消息类型
                if (chatHistory.getMessageType().equals(ChatHistoryMessageTypeEnum.USER.getValue())) {
                    // 封装为用户消息类型存储到记忆库
                    memory.add(UserMessage.from(chatHistory.getMessage()));
                } else if (chatHistory.getMessageType().equals(ChatHistoryMessageTypeEnum.AI.getValue())) {
                    // 封装为AI消息类型存储到记忆库
                    memory.add(AiMessage.from(chatHistory.getMessage()));
                }
                loadCount++;
            }
            log.info("加载对话历史到内存, appId={}, count={},", appId, loadCount);
            return loadCount;
        } catch (Exception e) {
            log.error("加载对话历史到内存失败, appId={}, error={}", appId, e.getMessage(), e);
            return 0;
        }
    }
}
