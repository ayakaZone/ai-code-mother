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
public class FileDeleteTool extends BaseTool {

    /**
     * 删除指定路径的文件
     *
     * @param relativeFilePath 文件相对路径
     * @param appId            应用ID
     * @return 删除结果
     */
    @Tool("删除指定路径的文件")
    public String deleteFile(@P("删除指定文件的相对路径") String relativeFilePath, @ToolMemoryId Long appId) {
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
            // 判断文件是否存在
            if (!Files.exists(path)) {
                return "警告：文件不存在，无需删除" + relativeFilePath;
            }
            // 判断是不是文件
            if (!Files.isRegularFile(path)) {
                return "警告：指定路径不是文件，无法删除" + relativeFilePath;
            }
            // 获取文件名
            String fileName = path.getFileName().toString();
            // 安全检查，不能删除重要文件
            if (isImportantFile(fileName)) {
                return "错误：不能删除重要文件" + relativeFilePath;
            }
            // 执行删除文件
            Files.delete(path);
            log.info("文件删除成功：{}", relativeFilePath);
            return "文件删除成功：" + relativeFilePath;
        } catch (IOException e) {
            String errorMessage = "文件删除失败：" + relativeFilePath + "，错误：" + e.getMessage();
            log.error(errorMessage, e);
            return errorMessage;
        }
    }

    /**
     * 判断是否是重要文件
     *
     * @param fileName 文件名
     * @return 是否是重要文件
     */
    private boolean isImportantFile(String fileName) {
        // 定义重要文件名
        String[] importantFiles = {
                "package.json", "package-lock.json", "yarn.lock", "pnpm-lock.yaml",
                "vite.config.js", "vite.config.ts", "vue.config.js",
                "tsconfig.json", "tsconfig.app.json", "tsconfig.node.json",
                "index.html", "main.js", "main.ts", "App.vue", ".gitignore", "README.md"
        };
        for (String important : importantFiles) {
            if (fileName.equals(important)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取工具名称
     *
     * @return 工具名称
     */
    @Override
    public String getToolName() {
        return "deleteFile";
    }

    /**
     * 获取工具显示名称
     *
     * @return 工具显示名称
     */
    @Override
    public String getDisplayName() {
        return "删除文件";
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

