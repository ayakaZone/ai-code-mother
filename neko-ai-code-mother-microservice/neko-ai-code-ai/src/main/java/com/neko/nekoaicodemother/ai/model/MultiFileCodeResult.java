package com.neko.nekoaicodemother.ai.model;

import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

/**
 * AI 生成多文件代码实体类
 */
@Data
@Description("生成多个代码文件的实体类")
public class MultiFileCodeResult {

    /**
     * HTML 代码
     */
    @Description("HTML 代码")
    private String htmlCode;

    /**
     * CSS 代码
     */
    @Description("CSS 代码")
    private String cssCode;

    /**
     * JS 代码
     */
    @Description("JS 代码")
    private String jsCode;

    /**
     * 描述
     */
    @Description("生成代码的描述")
    private String description;
}
