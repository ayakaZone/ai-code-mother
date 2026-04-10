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
import java.nio.file.StandardOpenOption;

@Slf4j
@Component
public class FileModifyTool extends BaseTool{

    @Tool("修改指定文件内容，用新内容替换指定的旧内容")
    public String modifyFile(@P("文件的相对路径") String relativeFilePath,
                             @P("要替换的旧内容") String oldContent,
                             @P("要替换的新内容") String newContent,
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
            // 读取文件内容为字符串
            String originalContent = Files.readString(path);
            if (!originalContent.contains(oldContent)) {
                return "警告：文件中未找到需要替换的就内容" + relativeFilePath;
            }
            // 替换内容
            String modifiedContent = originalContent.replace(oldContent, newContent);
            if (originalContent.equals(modifiedContent)) {
                return "提示：替换后文件未发生变化" + relativeFilePath;
            }
            // 写入文件
            Files.writeString(path, modifiedContent, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            log.info("文件修改成功, path={}", path.toAbsolutePath());
            return "文件修改成功：" + relativeFilePath;
        } catch (IOException e) {
            String errorMessage = "文件修改失败：" + relativeFilePath + "，错误：" + e.getMessage();
            log.error(errorMessage, e);
            return errorMessage;
        }
    }

    /**
     * 获取工具名称
     * @return 工具名称
     */
    @Override
    public String getToolName() {
        return "modifyFile";
    }

    /**
     * 获取工具显示名称
     * @return 工具显示名称
     */
    @Override
    public String getDisplayName() {
        return "修改文件";
    }

    /**
     * 生成工具执行结果信息
     * @param arguments 工具参数
     * @return 工具执行结果信息
     */
    @Override
    public String generateToolExecuteResponse(JSONObject arguments) {
        // 获取文件相对路径
        String relativePath = arguments.getStr("relativeFilePath");
        // 获取需要修改的旧内容
        String oldContent = arguments.getStr("oldContent");
        // 获取需要修改的新内容
        String newContent = arguments.getStr("newContent");
        return String.format("""
                [工具调用] %s %s
                
                替换前：
                ```
                %s
                ```
                
                替换后
                ```
                %s
                ```
                """, getDisplayName(), relativePath, oldContent, newContent);
    }
}
