package com.neko.nekoaicodemother.ai.tools;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import com.neko.nekoaicodemother.constant.AppConstant;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
public class FileDirReadTool extends BaseTool {

    /**
     * 需要忽略的文件和目录
     */
    private static final Set<String> IGNORED_NAMES = Set.of(
            "node_modules", ".git", "dist", "build", ".DS_Store", ".env", "target", ".mvn", ".idea", ".vscode", "coverage"
    );

    /**
     * 需要忽略的文件扩展名
     */
    private static final Set<String> IGNORED_EXTENSIONS = Set.of(".log", ".tmp", ".cache", ".lock");

    /**
     * 读取文件目录结构，获取指定目录下的所有文件和子目录信息
     *
     * @param relativeDirPath 目录的相对路径，为空则读取整个项目结构
     * @param appId           应用ID
     * @return 文件目录结构
     */
    @Tool("读取文件目录结构，获取指定目录下的所有文件和子目录信息")
    public String readDir(@P("目录的相对路径，为空则读取整个项目结构") String relativeDirPath, @ToolMemoryId Long appId) {
        try {
            // 获取路径
            Path path = Paths.get(relativeDirPath == null ? "" : relativeDirPath);
            // 判断是不是绝对路径
            if (!path.isAbsolute()) {
                // 项目目录名称
                String projectDirName = "vue_project_" + appId;
                // 项目绝对路径
                Path projectRoot = Paths.get(AppConstant.CODE_OUTPUT_ROOT_DIR, projectDirName);
                // 指定目录的绝对路径，未指定则读取项目根目录
                path = projectRoot.resolve(relativeDirPath == null ? "" : relativeDirPath);
            }
            // 判断是否是目录
            File targetDir = path.toFile();
            if (!targetDir.exists() || !targetDir.isDirectory()) {
                return "错误：目录不存在或不是目录" + relativeDirPath;
            }
            StringBuilder structure = new StringBuilder();
            structure.append("项目目录结构:\n");
            // 递归指定目录的所有文件
            List<File> allFiles = FileUtil.loopFiles(targetDir, file -> shouldIgnore(file.getName()));
            // 按路径深度和名称排序显示
            allFiles.stream().sorted((f1, f2) -> {
                // 比较文件相对于目录的深度进行排序
                int depth1 = getRelativeDepth(targetDir, f1);
                int depth2 = getRelativeDepth(targetDir, f2);
                if (depth1 != depth2) {
                    return Integer.compare(depth1, depth2);
                }
                // 深度相同按字母排序
                return f1.getPath().compareTo(f2.getPath());
            }).forEach(file -> {
                // 取出排好序的每一个文件名称
                int depth = getRelativeDepth(targetDir, file);
                // 根据深度缩进
                String indent = "  ".repeat(depth);
                // 添加文件名
                structure.append(indent).append(file.getName());
            });
            return structure.toString();
        } catch (Exception e) {
            String errorMessage = "读取目录结构失败：" + relativeDirPath + "，错误：" + e.getMessage();
            log.error(errorMessage, e);
            return errorMessage;
        }
    }

    /**
     * 获取文件相对于指定目录的深度计算文件相对于根目录的深度
     *
     * @param root 目录
     * @param file 文件
     * @return 文件相对于根目录的深度
     */
    private int getRelativeDepth(File root, File file) {
        Path rootPath = root.toPath();
        Path filePath = file.toPath();
        return rootPath.relativize(filePath).getNameCount() - 1;
    }

    /**
     * 判断文件是否需要忽略
     *
     * @param fileName 文件名
     * @return 是否需要忽略
     */
    private boolean shouldIgnore(String fileName) {
        // 检查文件名
        if (IGNORED_NAMES.contains(fileName)) {
            return true;
        }
        // 检查拓展名
        return IGNORED_EXTENSIONS.stream().anyMatch(fileName::endsWith);
    }

    /**
     * 获取工具名称
     *
     * @return 工具名称
     */
    @Override
    public String getToolName() {
        return "readDir";
    }

    /**
     * 获取工具显示名称
     *
     * @return 工具显示名称
     */
    @Override
    public String getDisplayName() {
        return "读取目录";
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
        if (StrUtil.isEmpty(relativePath)) {
            relativePath = "根目录";
        }
            return String.format("[工具调用] %s %s", getDisplayName(), relativePath);
    }
}

