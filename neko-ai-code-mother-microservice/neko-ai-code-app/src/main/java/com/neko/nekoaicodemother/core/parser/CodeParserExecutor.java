package com.neko.nekoaicodemother.core.parser;

import com.neko.nekoaicodemother.exception.BusinessException;
import com.neko.nekoaicodemother.exception.ErrorCode;
import com.neko.nekoaicodemother.model.enums.CodeGenTypeEnum;

/**
 * 代码解析执行器
 */
public class CodeParserExecutor {

    private static final HtmlCodeParser htmlCodeParser = new HtmlCodeParser();

    private static final MultiFileCodeParser multiFileCodeParser = new MultiFileCodeParser();

    /**
     * 执行代码解析入口
     * @param codeContent 解析的代码内容
     * @param codeGenTypeEnum 解析代码生成类型
     * @return 解析结果
     */
    public static Object ExecutorParser(String codeContent, CodeGenTypeEnum codeGenTypeEnum) {
        return switch (codeGenTypeEnum) {
            case HTML -> htmlCodeParser.parserCode(codeContent);
            case MULTI_FILE -> multiFileCodeParser.parserCode(codeContent);
            default -> throw new BusinessException(ErrorCode.SYSTEM_ERROR,"不支持的代码生成类型:" + codeGenTypeEnum);
        };
    }
}
