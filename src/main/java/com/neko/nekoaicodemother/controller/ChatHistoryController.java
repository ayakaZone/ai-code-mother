package com.neko.nekoaicodemother.controller;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.neko.nekoaicodemother.annotation.AuthCheck;
import com.neko.nekoaicodemother.common.BaseResponse;
import com.neko.nekoaicodemother.common.ResultUtils;
import com.neko.nekoaicodemother.constant.UserConstant;
import com.neko.nekoaicodemother.exception.ErrorCode;
import com.neko.nekoaicodemother.exception.ThrowUtils;
import com.neko.nekoaicodemother.model.dto.chathistory.ChatHistoryQueryRequest;
import com.neko.nekoaicodemother.model.entity.ChatHistory;
import com.neko.nekoaicodemother.model.entity.User;
import com.neko.nekoaicodemother.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import com.neko.nekoaicodemother.service.ChatHistoryService;

import java.time.LocalDateTime;

/**
 * 对话历史 控制层。
 *
 * @author mosoNeko
 */
@RestController
@RequestMapping("/chatHistory")
@Tag(name = "对话历史接口")
public class ChatHistoryController {

    @Resource
    private UserService userService;

    @Resource
    private ChatHistoryService chatHistoryService;

    /**
     * 获取应用对话历史
     *
     * @param appId          应用Id
     * @param pageSize       页大小
     * @param lastCreateTime 上次创建时间
     * @param request        请求
     * @return 应用对话历史
     */
    @GetMapping("/app/{appId}")
    @Operation(summary = "用户获取应用对话历史")
    public BaseResponse<Page<ChatHistory>> listAppChatHistory(@PathVariable Long appId,
                                                              @RequestParam(defaultValue = "10") int pageSize,
                                                              @RequestParam(required = false) LocalDateTime lastCreateTime,
                                                              HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Page<ChatHistory> chatHistoryPage = chatHistoryService.listAppChatHistoryByPage(appId, pageSize, lastCreateTime, loginUser);
        return ResultUtils.success(chatHistoryPage);
    }

    /**
     * 管理员获取对话历史分页列表
     * @param chatHistoryQueryRequest 查询条件
     * @return 列表
     */
    @PostMapping("/admin/list/page/vo")
    @Operation(summary = "管理员获取对话历史分页列表")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<ChatHistory>> listChatHistoryByPage(@RequestBody ChatHistoryQueryRequest chatHistoryQueryRequest) {
        // 校验参数
        ThrowUtils.throwIf(chatHistoryQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long pageNum = chatHistoryQueryRequest.getPageNum();
        long pageSize = chatHistoryQueryRequest.getPageSize();
        // 获取查询条件
        QueryWrapper queryWrapper = chatHistoryService.getQueryWrapper(chatHistoryQueryRequest);
        Page<ChatHistory> pageResult = chatHistoryService.page(Page.of(pageNum, pageSize), queryWrapper);
        return ResultUtils.success(pageResult);
    }

}
