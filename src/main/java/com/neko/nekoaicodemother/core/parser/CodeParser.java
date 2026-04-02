package com.neko.nekoaicodemother.core.parser;

public interface CodeParser<T> {

    /**
     * 解析代码统一入口
     * @param codeContent 代码内容
     * @return 解析结果
     */
    T parserCode(String codeContent);
}
