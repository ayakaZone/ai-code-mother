package com.neko.nekoaicodemother.core;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.neko.nekoaicodemother.ai.model.HtmlCodeResult;
import com.neko.nekoaicodemother.ai.model.MultiFileCodeResult;
import com.neko.nekoaicodemother.model.enums.CodeGenTypeEnum;

import java.io.File;
import java.nio.charset.StandardCharsets;

/**
 * 文件保存工具类
 */
@Deprecated
public class CodeFilesSaver {

    // 文件保存根目录
    public static final String FILE_SAVE_ROOT_DIR = System.getProperty("user.dir") + "/tmp/code_output";

    /**
     * 保存 HTML 代码结果
     *
     * @param htmlCodeResult HTML 代码结果
     * @return 保存后的文件
     */
    public static File saveHtmlCodeResult(HtmlCodeResult htmlCodeResult) {
        // 构建目录
        String baseDirPath = builderUnionDir(CodeGenTypeEnum.HTML.getValue());
        // 保存文件
        writeToFile(baseDirPath, "index.html", htmlCodeResult.getHtmlCode());
        // 返回文件
        return new File(baseDirPath);
    }

    /**
     * 保存多文件代码结果
     *
     * @param multiFileCodeResult 多文件代码结果
     * @return 保存后的文件
     */
    public static File saveMultiFileCodeResult(MultiFileCodeResult multiFileCodeResult) {
        // 构建目录
        String baseDirPath = builderUnionDir(CodeGenTypeEnum.MULTI_FILE.getValue());
        // 保存文件
        writeToFile(baseDirPath, "index.html", multiFileCodeResult.getHtmlCode());
        writeToFile(baseDirPath, "index.css", multiFileCodeResult.getCssCode());
        writeToFile(baseDirPath, "index.js", multiFileCodeResult.getJsCode());
        // 返回文件
        return new File(baseDirPath);
    }

    /**
     * 根据业务类型构建唯一目录
     *
     * @param bizType 业务类型
     * @return 唯一目录
     */
    private static String builderUnionDir(String bizType) {
        // 构建唯一目录名称
        String unionDirName = StrUtil.format("{}_{}", bizType, IdUtil.getSnowflakeNextIdStr());
        String dirPath = FILE_SAVE_ROOT_DIR + File.separator + unionDirName;
        // 创建目录
        FileUtil.mkdir(dirPath);
        return dirPath;
    }

    /**
     * 保存文件
     *
     * @param dirPath  目录路径
     * @param fileName 文件名
     * @param content  文件内容
     */
    private static void writeToFile(String dirPath, String fileName, String content) {
        // 构建路径
        String filePath = dirPath + File.separator + fileName;
        // 写入文件
        FileUtil.writeString(content, filePath, StandardCharsets.UTF_8);
    }
}
