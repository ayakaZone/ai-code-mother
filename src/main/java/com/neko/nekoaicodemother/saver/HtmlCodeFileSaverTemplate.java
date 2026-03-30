package com.neko.nekoaicodemother.saver;

import cn.hutool.core.util.StrUtil;
import com.neko.nekoaicodemother.ai.model.HtmlCodeResult;
import com.neko.nekoaicodemother.exception.BusinessException;
import com.neko.nekoaicodemother.exception.ErrorCode;
import com.neko.nekoaicodemother.model.enums.CodeGenTypeEnum;

public class HtmlCodeFileSaverTemplate extends CodeFileSaverTemplate<HtmlCodeResult>{
    /**
     * 获取代码类型
     * @return 代码类型枚举
     */
    @Override
    protected CodeGenTypeEnum getCodeType() {
        return CodeGenTypeEnum.HTML;
    }

    /**
     * 保存文件
     * @param result 结果
     * @param baseDirPath 基础目录路径
     */
    @Override
    protected void saveFiles(HtmlCodeResult result, String baseDirPath) {
        writeToFile(baseDirPath, "index.html", result.getHtmlCode());
    }

    /**
     * 校验输入
     * @param result 输入结果
     */
    @Override
    protected void validateInput(HtmlCodeResult result) {
        super.validateInput(result);
        // 校验 HTML 代码不能为空
        if (StrUtil.isBlank(result.getHtmlCode())) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "HTML 代码不能为空");
        }
    }
}
