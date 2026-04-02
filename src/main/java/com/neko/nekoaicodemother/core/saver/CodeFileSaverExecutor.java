package com.neko.nekoaicodemother.core.saver;

import com.neko.nekoaicodemother.ai.model.HtmlCodeResult;
import com.neko.nekoaicodemother.ai.model.MultiFileCodeResult;
import com.neko.nekoaicodemother.exception.BusinessException;
import com.neko.nekoaicodemother.exception.ErrorCode;
import com.neko.nekoaicodemother.model.enums.CodeGenTypeEnum;

import java.io.File;

/**
 * 代码文件保存执行器
 */
public class CodeFileSaverExecutor {

    public static final HtmlCodeFileSaverTemplate htmlCodeFileSaver = new HtmlCodeFileSaverTemplate();

    public static final MultiFileCodeFileSaverTemplate multiFileCodeFileSaver = new MultiFileCodeFileSaverTemplate();

    /**
     * 执行生成代码文件的保存
     * @param codeResult 生成的代码结果
     * @param codeGenType 代码生成类型
     * @return 保存后的文件
     */
    public static File executeSaver(Object codeResult, CodeGenTypeEnum codeGenType, long appId){
        return switch (codeGenType) {
            case HTML -> htmlCodeFileSaver.saveCode((HtmlCodeResult) codeResult, appId);
            case MULTI_FILE -> multiFileCodeFileSaver.saveCode((MultiFileCodeResult) codeResult, appId);
            default -> throw new BusinessException(ErrorCode.SYSTEM_ERROR,"不支持的代码生成类型:" + codeGenType);
        };
    }
}
