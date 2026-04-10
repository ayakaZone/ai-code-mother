package com.neko.nekoaicodemother.service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.neko.nekoaicodemother.model.dto.app.AppAddRequest;
import com.neko.nekoaicodemother.model.dto.app.AppQueryRequest;
import com.neko.nekoaicodemother.model.entity.App;
import com.neko.nekoaicodemother.model.entity.User;
import com.neko.nekoaicodemother.model.vo.app.AppVO;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 应用 服务层。
 *
 * @author mosoNeko
 */
public interface AppService extends IService<App> {

    /**
     * 应用部署
     * @param appId 应用id
     * @param loginUser 当前登录用户
     * @return 部署结果
     */
    String deployApp(Long appId, User loginUser);

    /**
     * 异步生成应用截图并保存到数据库
     * @param appId 应用id
     * @param appDeployUrl 应用部署地址
     */
    void generateAppScreenshotAsync(Long appId, String appDeployUrl);

    /**
     * AI 生成应用代码准入口
     * @param appId 应用id
     * @param userMessage 用户提示词
     * @param loginUser 当前登录用户
     * @return AI 生成应用代码流
     */
    Flux<String> chatTOGenCode(Long appId, String userMessage, User loginUser);

    /**
     * 获取脱敏后的应用详情
     *
     * @param app 应用
     * @return 脱敏后的应用详情
     */
    AppVO getAppVO(App app);

    /**
     * 获取查询条件
     *
     * @param appQueryRequest 查询条件
     * @return 查询条件
     */
    QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest);

    /**
     * 获取脱敏后的应用列表
     * @param appList 应用列表
     * @return 脱敏后的应用列表
     */
    List<AppVO> getAppVOList(List<App> appList);

    /**
     * 创建应用
     * @param appAddRequest 应用创建请求
     * @param loginUser 当前登录用户
     * @return 应用id
     */
    Long createApp(AppAddRequest appAddRequest, User loginUser);
}
