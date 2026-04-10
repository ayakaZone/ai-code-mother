package com.neko.nekoaicodemother.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.neko.nekoaicodemother.ai.AiCodeGenTypeRoutingService;
import com.neko.nekoaicodemother.ai.AiCodeGenTypeRoutingServiceFactory;
import com.neko.nekoaicodemother.constant.AppConstant;
import com.neko.nekoaicodemother.core.AiCodeGeneratorFacade;
import com.neko.nekoaicodemother.core.builder.VueProjectBuilder;
import com.neko.nekoaicodemother.core.handler.StreamHandlerExecutor;
import com.neko.nekoaicodemother.exception.BusinessException;
import com.neko.nekoaicodemother.exception.ErrorCode;
import com.neko.nekoaicodemother.exception.ThrowUtils;
import com.neko.nekoaicodemother.innerservice.InnerScreenshotService;
import com.neko.nekoaicodemother.innerservice.InnerUserService;
import com.neko.nekoaicodemother.mapper.AppMapper;
import com.neko.nekoaicodemother.model.dto.app.AppAddRequest;
import com.neko.nekoaicodemother.model.dto.app.AppQueryRequest;
import com.neko.nekoaicodemother.model.entity.App;
import com.neko.nekoaicodemother.model.entity.User;
import com.neko.nekoaicodemother.model.enums.ChatHistoryMessageTypeEnum;
import com.neko.nekoaicodemother.model.enums.CodeGenTypeEnum;
import com.neko.nekoaicodemother.model.vo.app.AppVO;
import com.neko.nekoaicodemother.model.vo.user.UserVO;
import com.neko.nekoaicodemother.service.AppService;
import com.neko.nekoaicodemother.service.ChatHistoryService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 应用 服务层实现。
 *
 * @author mosoNeko
 */
@Service
@Slf4j
public class AppServiceImpl extends ServiceImpl<AppMapper, App> implements AppService {

    @Resource
    @DubboReference
    private InnerUserService userService;

    @Resource
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private StreamHandlerExecutor streamHandlerExecutor;

    @Resource
    private VueProjectBuilder vueProjectBuilder;

    @Resource
    @DubboReference
    private InnerScreenshotService screenshotService;

    @Resource
    private AiCodeGenTypeRoutingServiceFactory aiCodeGenTypeRoutingServiceFactory;

    /**
     * 部署应用
     *
     * @param appId     应用id
     * @param loginUser 当前登录用户
     * @return 访问地址
     */
    @Override
    public String deployApp(Long appId, User loginUser) {
        // 参数校验
        ThrowUtils.throwIf(appId <= 0, ErrorCode.PARAMS_ERROR, "应用Id不能为空");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        // 应用是否存在
        App app = getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 用户部署权限校验（仅本人）
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限部署该应用");
        }
        // 检查应用是否已有 deployKey 规定：6位随机大小写字母和数字符号组成
        String deployKey = app.getDeployKey();
        if (StrUtil.isBlank(deployKey)) {
            // 没有就生成
            deployKey = RandomUtil.randomString(6);
        }
        // 获取应用类型作为命名前缀,拼接生成代码源文件路径
        String codeGenType = app.getCodeGenType();
        String sourceDirName = codeGenType + "_" + appId;
        String sourceDirPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + sourceDirName;
        // 检查源文件是否存在
        File sourceDir = new File(sourceDirPath);
        if (!sourceDir.exists() || !sourceDir.isDirectory()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "应用代码不存在，请重新生成代码");
        }
        // 如果是 Vue 项目需要再打包构建一次，确保是最新状态
        if (codeGenType.equals(CodeGenTypeEnum.VUE_PROJECT.getValue())) {
            // Vue 需要构建
            vueProjectBuilder.buildProjectAsync(sourceDirPath);
            ThrowUtils.throwIf(!FileUtil.exist(sourceDirPath), ErrorCode.SYSTEM_ERROR, "Vue 项目构建失败，请检查依赖和代码");
            // 检查是否生成 dist 目录
            File disDir = new File(sourceDirPath, "dist");
            ThrowUtils.throwIf(!disDir.exists(), ErrorCode.SYSTEM_ERROR, "Vue 项目构建完成，dist 目录不存在");
            // 将 dist 目录作为部署源
            sourceDir = disDir;
            log.info("Vue 项目构建完成，dist 目录：{}", disDir.getAbsolutePath());
        }
        // 将源文件复制到部署目录下
        String deployDirPath = AppConstant.CODE_DEPLOY_ROOT_DIR + File.separator + deployKey;
        try {
            FileUtil.copyContent(sourceDir, new File(deployDirPath), true);
        } catch (IORuntimeException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "应用部署失败" + e.getMessage());
        }
        // 更新 app 部署信息
        App updateApp = new App();
        updateApp.setId(appId);
        updateApp.setDeployKey(deployKey);
        updateApp.setDeployedTime(LocalDateTime.now());
        boolean updateResult = updateById(updateApp);
        ThrowUtils.throwIf(!updateResult, ErrorCode.OPERATION_ERROR, "更新应用部署信息失败");
        // 获取应用访问地址
        String appDeployUrl = String.format("%s/%s/", AppConstant.CODE_DEPLOY_HOST, deployKey);
        // 异步上传应用截图到 COS 中
        generateAppScreenshotAsync(appId, appDeployUrl);
        return appDeployUrl;
    }

    /**
     * 异步生成应用截图，并将访问地址保存到数据库
     *
     * @param appId        应用id
     * @param appDeployUrl 应用访问地址
     */
    @Override
    public void generateAppScreenshotAsync(Long appId, String appDeployUrl) {
        // 使用虚拟线程异步调用
        Thread.startVirtualThread(() -> {
            // 上传截图
            String screenshotCosUrl = screenshotService.generateAndUploadScreenshot(appDeployUrl);
            // 更新数据库的应用封面
            App updateApp = new App();
            updateApp.setId(appId);
            updateApp.setCover(screenshotCosUrl);
            boolean updateResult = updateById(updateApp);
            ThrowUtils.throwIf(!updateResult, ErrorCode.OPERATION_ERROR, "更新应用封面失败");
        });
    }

    /**
     * 创建应用
     * @param appAddRequest 应用创建请求
     * @param loginUser 当前登录用户
     * @return 应用id
     */
    @Override
    public Long createApp(AppAddRequest appAddRequest, User loginUser) {
        String initPrompt = appAddRequest.getInitPrompt();
        ThrowUtils.throwIf(initPrompt == null, ErrorCode.PARAMS_ERROR, "初始化 Prompt 不能为空");
        // 构造app对象
        App app = new App();
        BeanUtil.copyProperties(appAddRequest, app);
        /// 初始化
        // appName 规定：默认取初始化提示词前 12 位
        app.setAppName(initPrompt.substring(0, Math.min(initPrompt.length(), 12)));
        // 创建者
        app.setUserId(loginUser.getId());
        // 使用 AI 智能选择生成代码类型
        AiCodeGenTypeRoutingService routingService = aiCodeGenTypeRoutingServiceFactory.createAiCodeGenTypeRoutingService();
        CodeGenTypeEnum codeGenTypeEnum = routingService.routeCodeGenType(initPrompt);
        app.setCodeGenType(codeGenTypeEnum.getValue());
        // 插入数据库
        boolean saveResult = this.save(app);
        ThrowUtils.throwIf(!saveResult, ErrorCode.OPERATION_ERROR);
        log.info("应用创建成功，id：{}，类型：{}", app.getId(), codeGenTypeEnum.getValue());
        return app.getId();
    }

    /**
     * AI 生成应用代码准入口
     *
     * @param appId       应用id
     * @param userMessage 用户提示词
     * @param loginUser   当前登录用户
     * @return AI 生成应用代码流
     */
    @Override
    public Flux<String> chatTOGenCode(Long appId, String userMessage, User loginUser) {
        // 校验参数
        ThrowUtils.throwIf(appId <= 0, ErrorCode.PARAMS_ERROR, "应用Id不能为空");
        ThrowUtils.throwIf(userMessage == null, ErrorCode.PARAMS_ERROR, "用户提示词不能为空");
        // 生成应用是否存在
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 用户是否有权限（用户只能与自己生成应用的AI对话）
        if (!loginUser.getId().equals(app.getUserId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限访问该应用");
        }
        // 应用生成类型
        String codeGenType = app.getCodeGenType();
        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(codeGenType);
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "不支持的代码生成类型");
        }
        // 保存用户对话消息到对话历史中
        boolean saveResult = chatHistoryService.addChatHistory
                (appId, userMessage, ChatHistoryMessageTypeEnum.USER.getValue(), loginUser.getId());
        ThrowUtils.throwIf(!saveResult, ErrorCode.OPERATION_ERROR, "保存用户对话消息失败");
        // 调用AI生成代码（1.生成 2.保存）门面类
        Flux<String> contentFlux = aiCodeGeneratorFacade.GeneratorAndSaveStream(userMessage, codeGenTypeEnum, appId);
        // 调用流处理器执行器，收集 AI 的响应内容并保存到对话历史
        return streamHandlerExecutor.doExecutor(contentFlux, chatHistoryService, appId, loginUser, codeGenTypeEnum);
    }

    /**
     * 获取应用视图对象
     *
     * @param app 应用
     * @return 应用视图对象
     */
    @Override
    public AppVO getAppVO(App app) {
        // 校验
        if (app == null) {
            return null;
        }
        // 构造查询对象
        AppVO appVO = new AppVO();
        BeanUtil.copyProperties(app, appVO);
        // 关联查询应用的创建用户脱敏信息
        Long userId = app.getUserId();
        if (userId != null) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            appVO.setUser(userVO);
        }
        return appVO;
    }

    /**
     * 获取查询条件
     *
     * @param appQueryRequest 查询条件
     * @return 查询条件
     */
    @Override
    public QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest) {
        // 校验参数
        if (appQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        // 获取查询条件
        Long id = appQueryRequest.getId();
        String appName = appQueryRequest.getAppName();
        String cover = appQueryRequest.getCover();
        String initPrompt = appQueryRequest.getInitPrompt();
        String codeGenType = appQueryRequest.getCodeGenType();
        String deployKey = appQueryRequest.getDeployKey();
        Integer priority = appQueryRequest.getPriority();
        Long userId = appQueryRequest.getUserId();
        String sortField = appQueryRequest.getSortField();
        String sortOrder = appQueryRequest.getSortOrder();
        // 创建查询条件并返回
        return QueryWrapper.create().eq("id", id).eq("appName", appName).eq("cover", cover).eq("initPrompt", initPrompt).eq("codeGenType", codeGenType).eq("deployKey", deployKey).eq("priority", priority).eq("userId", userId).orderBy(sortField, "ascend".equals(sortOrder));
    }

    /**
     * 获取应用视图对象列表
     *
     * @param appList 应用列表
     * @return 应用视图对象列表
     */
    @Override
    public List<AppVO> getAppVOList(List<App> appList) {
        // 校验参数
        if (CollUtil.isEmpty(appList)) {
            return new ArrayList<>();
        }
        /// AppVO 中包含 UserVO 需要对 app内的 user 进行脱敏
        // 从appList中获取所有的userId，因为多个app可能由同一个user创建，使用set存储userId过滤相同信息
        Set<Long> userIdSet = appList.stream().map(App::getUserId).collect(Collectors.toSet());
        // 获取userList
        List<User> userList = userService.listByIds(userIdSet);
        // 获取Map<userId, userVO>
        Map<Long, UserVO> userVOMap = userList.stream().collect(Collectors.toMap(User::getId, user -> userService.getUserVO(user)));
        // 获取AppVOList 补充AppVO中的userVO
        return appList.stream().map(app -> {
            // App 转 AppVO
            AppVO appVO = getAppVO(app);
            // 从 map 中获取 UserVO
            UserVO userVO = userVOMap.get(app.getUserId());
            // 补充
            appVO.setUser(userVO);
            return appVO;
        }).toList();
    }

    /**
     * 删除应用并删除关联应用的对话历史
     *
     * @param id 应用Id
     * @return 是否删除成功
     */
    @Override
    public boolean removeById(Serializable id) {
        // 校验参数
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        long appId = Long.parseLong(id.toString());
        // 判断应用是否存在
        App app = getById(appId);
        if (app == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        }
        // 删除应用关联的对话历史
        try {
            boolean deleteResult = chatHistoryService.deleteChatHistory(appId);
        } catch (Exception e) {
            log.error("删除对话历史失败：{}", e.getMessage());
        }
        // 删除应用
        return super.removeById(id);
    }
}
