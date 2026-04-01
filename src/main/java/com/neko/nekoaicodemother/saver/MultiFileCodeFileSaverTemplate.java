package com.neko.nekoaicodemother.saver;

import cn.hutool.core.util.StrUtil;
import com.neko.nekoaicodemother.ai.model.MultiFileCodeResult;
import com.neko.nekoaicodemother.exception.BusinessException;
import com.neko.nekoaicodemother.exception.ErrorCode;
import com.neko.nekoaicodemother.model.enums.CodeGenTypeEnum;

public class MultiFileCodeFileSaverTemplate extends CodeFileSaverTemplate<MultiFileCodeResult>{
    /**
     * 获取代码类型
     * @return 代码类型枚举
     */
    @Override
    protected CodeGenTypeEnum getCodeType() {
        return CodeGenTypeEnum.MULTI_FILE;
    }

    /**
     * 保存文件
     * @param result 结果
     * @param baseDirPath 基础目录路径
     */
    @Override
    protected void saveFiles(MultiFileCodeResult result, String baseDirPath) {
        // 保存HTML文件
        writeToFile(baseDirPath, "index.html", result.getHtmlCode());
        // 保存CSS文件
        writeToFile(baseDirPath, "style.css", result.getCssCode());
        // 保存JS文件
        writeToFile(baseDirPath, "script.js", result.getJsCode());
    }

    /**
     * 校验生成代码结果
     * @param result 生成的代码结果
     */
    @Override
    protected void validateInput(MultiFileCodeResult result) {
        super.validateInput(result);
        if (StrUtil.isBlank(result.getHtmlCode())) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "HTML代码不能为空");
        }
    }
}
