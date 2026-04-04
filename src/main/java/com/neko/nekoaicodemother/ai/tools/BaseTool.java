package com.neko.nekoaicodemother.ai.tools;

import cn.hutool.json.JSONObject;

public abstract class BaseTool {

    /**
     * 获取工具名称
     *
     * @return 工具名称
     */
    public abstract String getToolName();

    /**
     * 获取工具显示名称
     *
     * @return 工具显示名称
     */
    public abstract String getDisplayName();

    /**
     * 生成工具调用请求信息
     *
     * @return 工具调用请求信息
     */
    public String generateToolRequestResponse() {
        return String.format("\n\n[选择工具] %s\n\n", getDisplayName());
    }

    /**
     * 生成工具执行结果信息
     *
     * @return 工具执行结果信息
     */
    public abstract String generateToolExecuteResponse(JSONObject arguments);
}
