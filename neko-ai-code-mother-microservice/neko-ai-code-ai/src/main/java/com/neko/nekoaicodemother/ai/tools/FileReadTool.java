package com.neko.nekoaicodemother.ai.tools;

import cn.hutool.json.JSONObject;
import com.neko.nekoaicodemother.constant.AppConstant;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Component
public class FileReadTool extends BaseTool {

    @Tool("读取指定路径的文件内容")
    public String readFile(@P("文件的相对路径") String relativeFilePath,
                           @ToolMemoryId Long appId) {
        try {
            // 获取路径
            Path path = Paths.get(relativeFilePath);
            // 判断是不是绝对路径
            if (!path.isAbsolute()) {
                // 项目目录名称
                String projectDirName = "vue_project_" + appId;
                // 项目绝对路径
                Path projectRoot = Paths.get(AppConstant.CODE_OUTPUT_ROOT_DIR, projectDirName);
                // 文件绝对路径
                path = projectRoot.resolve(relativeFilePath);
            }
            // 判断文件是否存在和是否是文件
            if (!Files.exists(path) || !Files.isRegularFile(path)) {
                return "错误：文件不存在或不是文件" + relativeFilePath;
            }
            return Files.readString(path);
        } catch (IOException e) {
            String errorMessage = "读取文件失败：" + relativeFilePath + "，错误：" + e.getMessage();
            log.error(errorMessage, e);
            return errorMessage;
        }
    }

    /**
     * 获取工具名称
     *
     * @return 工具名称
     */
    @Override
    public String getToolName() {
        return "readFile";
    }

    /**
     * 获取工具显示名称
     *
     * @return 工具显示名称
     */
    @Override
    public String getDisplayName() {
        return "读取文件";
    }

    /**
     * 生成工具执行结果信息
     *
     * @param arguments 工具参数
     * @return 工具执行结果信息
     */
    @Override
    public String generateToolExecuteResponse(JSONObject arguments) {
        // 获取文件相对路径
        String relativePath = arguments.getStr("relativeFilePath");
        return String.format("[工具调用] %s %s", getDisplayName(), relativePath);
    }
}
