package com.neko.nekoaicodemother.ai.tools;

import com.neko.nekoaicodemother.constant.AppConstant;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

@Slf4j
public class FileWriterTool {

    /**
     * AI 写入文件工具
     *
     * @param relativeFilePath 文件相对路径
     * @param content          文件内容
     * @param appId            应用 ID
     * @return 文件写入结果
     */
    @Tool("写入文件到指定路径")
    public String writeFile(@P("文件的相对路径") String relativeFilePath, @P("写入文件的内容") String content, @ToolMemoryId Long appId) {
        try {
            // 保存为路径
            Path path = Paths.get(relativeFilePath);
            // 判断是否是绝对路径
            if (!path.isAbsolute()) {
                // 转换为绝对路径
                String projectDirName = "vue_project_" + appId;
                Path projectRoot = Paths.get(AppConstant.CODE_OUTPUT_ROOT_DIR, projectDirName);
                path = projectRoot.resolve(relativeFilePath);
            }
            // 获取文件父目录
            Path parentDir = path.getParent();
            if (!parentDir.toFile().exists()) {
                // 创建父目录
                Files.createDirectories(parentDir);
            }
            // 写入文件，文件存在就覆写
            Files.write(path, content.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            log.info("写入文件成功, path={}", path.toAbsolutePath());
            // 返回相对路径
            return "写入文件成功：" + relativeFilePath;
        } catch (IOException e) {
            String errorMessage = "写入文件失败：" + relativeFilePath + "，错误：" + e.getMessage();
            log.error(errorMessage, e);
            return errorMessage;
        }
    }
}
