package com.neko.nekoaicodemother.saver;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.neko.nekoaicodemother.exception.BusinessException;
import com.neko.nekoaicodemother.exception.ErrorCode;
import com.neko.nekoaicodemother.model.enums.CodeGenTypeEnum;

import java.io.File;
import java.nio.charset.StandardCharsets;

/**
 * 抽象代码文件保存模板方式类
 *
 * @param <T>
 */
public abstract class CodeFileSaverTemplate<T> {

    protected static final String FILE_SAVE_ROOT_DIR = System.getProperty("user.dir") + "/tmp/code_output";

    /**
     * 保存代码模板方式
     * @param result 生成的代码结果
     * @return 文件
     */
    public final File saveCode(T result) {
        // 校验生成代码结果
        validateInput(result);
        // 构建唯一目录
        String baseDirPath = builderUnionDir();
        // 保存代码
        saveFiles(result, baseDirPath);
        // 返回目录文件对象
        return new File(baseDirPath);
    }

    /**
     * 校验生成的代码
     *
     * @param result 生成的代码结果
     */
    protected void validateInput(T result) {
        if (result == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成代码不能为空");
        }
    }

    /**
     * 根据业务类型构建唯一目录
     *
     * @return 唯一目录
     */
    protected final String builderUnionDir() {
        // 获取生成代码类型
        String codeType = getCodeType().getValue();
        // 构建唯一目录名称
        String unionDirName = StrUtil.format("{}_{}", codeType, IdUtil.getSnowflakeNextIdStr());
        String dirPath = FILE_SAVE_ROOT_DIR + File.separator + unionDirName;
        // 创建目录
        FileUtil.mkdir(dirPath);
        return dirPath;
    }

    /**
     * 获取生成代码类型(由子类具体实现)
     *
     * @return 生成代码类型枚举
     */
    protected abstract CodeGenTypeEnum getCodeType();

    /**
     * 保存代码的具体实现（由子类实现）
     * @param result 生成的代码结果
     * @param baseDirPath 唯一目录
     */
    protected abstract void saveFiles(T result, String baseDirPath);

    /**
     * 保存文件
     *
     * @param dirPath  目录路径
     * @param fileName 文件名
     * @param content  文件内容
     */
    protected final void writeToFile(String dirPath, String fileName, String content) {
        if (StrUtil.isNotBlank(content)) {
            // 构建路径
            String filePath = dirPath + File.separator + fileName;
            // 写入文件
            FileUtil.writeString(content, filePath, StandardCharsets.UTF_8);
        }
    }
}
