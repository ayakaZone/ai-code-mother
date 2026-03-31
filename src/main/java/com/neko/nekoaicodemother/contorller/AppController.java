package com.neko.nekoaicodemother.contorller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.neko.nekoaicodemother.annotation.AuthCheck;
import com.neko.nekoaicodemother.common.BaseResponse;
import com.neko.nekoaicodemother.common.DeleteRequest;
import com.neko.nekoaicodemother.common.ResultUtils;
import com.neko.nekoaicodemother.constant.UserConstant;
import com.neko.nekoaicodemother.constant.appConstant;
import com.neko.nekoaicodemother.exception.BusinessException;
import com.neko.nekoaicodemother.exception.ErrorCode;
import com.neko.nekoaicodemother.exception.ThrowUtils;
import com.neko.nekoaicodemother.model.dto.app.*;
import com.neko.nekoaicodemother.model.entity.App;
import com.neko.nekoaicodemother.model.entity.User;
import com.neko.nekoaicodemother.model.enums.CodeGenTypeEnum;
import com.neko.nekoaicodemother.model.enums.UserRoleEnum;
import com.neko.nekoaicodemother.model.vo.app.AppVO;
import com.neko.nekoaicodemother.service.AppService;
import com.neko.nekoaicodemother.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 应用 控制层。
 *
 * @author mosoNeko
 */
@RestController
@RequestMapping("/app")
@Tag(name = "应用接口")
public class AppController {

    @Resource
    private AppService appService;

    @Resource
    private UserService userService;

    /**
     * 应用部署。
     * @param appDeployRequest 应用部署请求
     * @param request 请求
     * @return 应用部署地址
     */
    @PostMapping("/deploy")
    @Operation(summary = "应用部署")
    public BaseResponse<String> deployApp(@RequestBody AppDeployRequest appDeployRequest, HttpServletRequest request) {
        // 校验参数
        ThrowUtils.throwIf(appDeployRequest == null, ErrorCode.PARAMS_ERROR);
        // 获取应用id
        Long appId = appDeployRequest.getAppId();
        ThrowUtils.throwIf(appId <= 0, ErrorCode.PARAMS_ERROR, "应用Id不能为空");
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        // 部署应用
        String deployUrl = appService.deployApp(appId, loginUser);
        return ResultUtils.success(deployUrl);
    }

    /**
     * AI 生成应用代码。
     *
     * @param appId       应用 Id
     * @param message 用户提示词
     * @param request     请求
     * @return 应用代码流
     */
    @GetMapping(value = "/chat/gen/code", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "AI 生成应用代码")
    public Flux<ServerSentEvent<String>> chatToGenCode(@RequestParam long appId,
                                                       @RequestParam String message,
                                                       HttpServletRequest request) {
        // 参数校验
        ThrowUtils.throwIf(message == null, ErrorCode.PARAMS_ERROR, "用户提示词不能为空");
        ThrowUtils.throwIf(appId <= 0, ErrorCode.PARAMS_ERROR, "应用Id不能为空");
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        // 调用AI生成应用代码的接口
        Flux<String> contentFlux = appService.chatTOGenCode(appId, message, loginUser);
        // 处理单个流
        return contentFlux
                .map(chunk -> {
                    // 使用 Map 包装 Ai 流的生成结果
                    Map<String, String> wrapper = Map.of("d", chunk);
                    // 转为 Json 字符串
                    String jsonData = JSONUtil.toJsonStr(wrapper);
                    return ServerSentEvent.<String>builder().data(jsonData).build();
                })
                // concatWith() 拼接流
                .concatWith(Mono.just(
                        // 发送结束事件
                        ServerSentEvent.<String>builder().event("done").data("").build()
                ));
    }

    /**
     * 创建应用。
     *
     * @param appAddRequest 应用添加请求
     * @param request       创建请求
     * @return 创建成功的应用 ID
     */
    @PostMapping("/add")
    @Operation(summary = "用户创建应用")
    public BaseResponse<Long> addApp(@RequestBody AppAddRequest appAddRequest, HttpServletRequest request) {
        // 参数校验
        ThrowUtils.throwIf(appAddRequest == null, ErrorCode.PARAMS_ERROR);
        String initPrompt = appAddRequest.getInitPrompt();
        ThrowUtils.throwIf(initPrompt == null, ErrorCode.PARAMS_ERROR, "初始化 Prompt 不能为空");
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        // 构造app对象
        App app = new App();
        BeanUtil.copyProperties(appAddRequest, app);
        /// 初始化
        // appName 规定：默认取初始化提示词前 12 位
        app.setAppName(initPrompt.substring(0, Math.min(initPrompt.length(), 12)));
        // 创建者
        app.setUserId(loginUser.getId());
        // 文件生成类型 规定：默认生成多文件类型 Multi_File
        app.setCodeGenType(CodeGenTypeEnum.MULTI_FILE.getValue());
        // 插入数据库
        boolean saveResult = appService.save(app);
        ThrowUtils.throwIf(!saveResult, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(app.getId());
    }

    /**
     * 删除应用。
     *
     * @param deleteRequest 删除请求
     * @param request       请求
     * @return 删除结果
     */
    @PostMapping("/delete")
    @Operation(summary = "用户删除应用")
    public BaseResponse<Boolean> deleteApp(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        // 校验参数
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 获取要删除的应用 id
        Long appId = deleteRequest.getId();
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        // 判断app是否存在
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        // 校验权限(仅本人或管理员)
        if (!app.getUserId().equals(loginUser.getId()) && !loginUser.getUserRole().equals(UserRoleEnum.ADMIN.getValue())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 逻辑删除
        boolean removeResult = appService.removeById(appId);
        ThrowUtils.throwIf(!removeResult, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 更新应用。
     *
     * @param appUpdateRequest 应用更新请求
     * @param request          请求
     * @return 更新结果
     */
    @PostMapping("/update")
    @Operation(summary = "用户更新应用")
    public BaseResponse<Boolean> updateApp(@RequestBody AppUpdateRequest appUpdateRequest, HttpServletRequest request) {
        // 校验参数
        if (appUpdateRequest == null || appUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        Long appId = appUpdateRequest.getId();
        // 判断app是否存在
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        // 校验权限(仅本人)
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 构造更新对象
        App updateApp = new App();
        BeanUtil.copyProperties(appUpdateRequest, updateApp);
        updateApp.setId(appId);
        updateApp.setAppName(appUpdateRequest.getAppName());
        // 设置编辑时间
        updateApp.setEditTime(LocalDateTime.now());
        // 更新app
        boolean updateResult = appService.updateById(updateApp);
        ThrowUtils.throwIf(!updateResult, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 获取应用详情。
     *
     * @param id 应用 id
     * @return 应用详情
     */
    @GetMapping("/get/vo")
    @Operation(summary = "用户获取应用详情")
    public BaseResponse<AppVO> getAppVO(long id) {
        // 校验
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询应用是否存在
        App app = appService.getById(id);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(appService.getAppVO(app));
    }

    /**
     * 获取用户应用列表（分页）。
     *
     * @param appQueryRequest 查询条件
     * @param request         请求
     * @return 用户应用列表（分页）
     */
    @PostMapping("/my/list/page/vo")
    @Operation(summary = "用户获取应用列表（分页）")
    public BaseResponse<Page<AppVO>> listMyAppVOByPage(@RequestBody AppQueryRequest appQueryRequest, HttpServletRequest request) {
        // 校验参数
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        // 限制查询页数
        long pageSize = appQueryRequest.getPageSize();
        ThrowUtils.throwIf(pageSize > 20, ErrorCode.PARAMS_ERROR);
        long pageNum = appQueryRequest.getPageNum();
        // 限制用户智能查询自己的应用
        appQueryRequest.setUserId(loginUser.getId());
        // 分页查询
        QueryWrapper queryWrapper = appService.getQueryWrapper(appQueryRequest);
        Page<App> appPage = appService.page(Page.of(pageNum, pageSize), queryWrapper);
        // 数据封装
        Page<AppVO> appVOPage = new Page<>(pageNum, pageSize, appPage.getTotalRow());
        List<AppVO> appVOList = appService.getAppVOList(appPage.getRecords());
        appVOPage.setRecords(appVOList);
        return ResultUtils.success(appVOPage);
    }

    /**
     * 获取精选应用列表（分页）。
     *
     * @param appQueryRequest 查询条件
     * @return 精选应用列表（分页）
     */
    @PostMapping("/good/list/page/vo")
    @Operation(summary = "用户获取精选应用列表（分页）")
    public BaseResponse<Page<AppVO>> listGoodAppVOByPage(@RequestBody AppQueryRequest appQueryRequest) {
        // 校验参数
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);
        // 限制查询页数
        long pageSize = appQueryRequest.getPageSize();
        ThrowUtils.throwIf(pageSize > 20, ErrorCode.PARAMS_ERROR);
        long pageNum = appQueryRequest.getPageNum();
        // 限制查询精选应用
        appQueryRequest.setPriority(appConstant.GOOD_APP_PRIORITY);
        // 分页查询
        QueryWrapper queryWrapper = appService.getQueryWrapper(appQueryRequest);
        Page<App> appPage = appService.page(Page.of(pageNum, pageSize), queryWrapper);
        // 数据封装
        Page<AppVO> appVOPage = new Page<>(pageNum, pageSize, appPage.getTotalRow());
        List<AppVO> appVOList = appService.getAppVOList(appPage.getRecords());
        appVOPage.setRecords(appVOList);
        return ResultUtils.success(appVOPage);
    }

    /**
     * 删除应用。
     *
     * @param deleteRequest 删除请求
     * @return 删除结果
     */
    @PostMapping("/admin/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @Operation(summary = "管理员删除应用")
    public BaseResponse<Boolean> deleteAppByAdmin(@RequestBody DeleteRequest deleteRequest) {
        // 校验参数
        ThrowUtils.throwIf(deleteRequest == null || deleteRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        Long appId = deleteRequest.getId();
        // 查询应用是否存在
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        // 删除应用
        boolean deleteResult = appService.removeById(appId);
        ThrowUtils.throwIf(!deleteResult, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 更新应用。
     *
     * @param appAdminUpdateRequest 更新请求
     * @return 更新结果
     */
    @PostMapping("/admin/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @Operation(summary = "管理员更新应用")
    public BaseResponse<Boolean> updateAppByAdmin(@RequestBody AppAdminUpdateRequest appAdminUpdateRequest) {
        // 校验参数
        ThrowUtils.throwIf(appAdminUpdateRequest == null || appAdminUpdateRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        Long appId = appAdminUpdateRequest.getId();
        // 获取应用是否存在
        App oldapp = appService.getById(appId);
        ThrowUtils.throwIf(oldapp == null, ErrorCode.NOT_FOUND_ERROR);
        App app = new App();
        BeanUtil.copyProperties(appAdminUpdateRequest, app);
        // 设置编辑时间
        app.setEditTime(LocalDateTime.now());
        boolean updateResult = appService.updateById(app);
        ThrowUtils.throwIf(!updateResult, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 获取所有应用列表（分页）。
     *
     * @param appQueryRequest 查询条件
     * @return 所有应用列表（分页）
     */
    @PostMapping("/admin/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @Operation(summary = "管理员获取所有应用列表（分页）")
    public BaseResponse<Page<AppVO>> listAppVOByPageByAdmin(@RequestBody AppQueryRequest appQueryRequest) {
        // 校验参数
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long pageNum = appQueryRequest.getPageNum();
        long pageSize = appQueryRequest.getPageSize();
        QueryWrapper queryWrapper = appService.getQueryWrapper(appQueryRequest);
        Page<App> appPage = appService.page(Page.of(pageNum, pageSize), queryWrapper);
        // 数据封装
        Page<AppVO> appVOPage = new Page<>(pageNum, pageSize, appPage.getTotalRow());
        List<AppVO> appVOList = appService.getAppVOList(appPage.getRecords());
        appVOPage.setRecords(appVOList);
        return ResultUtils.success(appVOPage);
    }

    /**
     * 获取应用详情。
     *
     * @param id 应用id
     * @return 应用详情
     */
    @GetMapping("/admin/get/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @Operation(summary = "管理员获取应用详情")
    public BaseResponse<AppVO> getAppVOByIdByAdmin(long id) {
        // 校验参数
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 获取应用是否存在
        App app = appService.getById(id);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        // 数据封装
        return ResultUtils.success(appService.getAppVO(app));
    }
}
