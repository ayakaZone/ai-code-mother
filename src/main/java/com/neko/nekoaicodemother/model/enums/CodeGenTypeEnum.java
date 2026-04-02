package com.neko.nekoaicodemother.model.enums;

import lombok.Getter;

@Getter
public enum CodeGenTypeEnum {

    /**
     * 代码生成类型枚举类
     */
    HTML("原生 HTML 模式", "html"),
    MULTI_FILE("原生多文件模式", "multi_file"),
    VUE_PROJECT("Vue 工程模式", "vue_project");

    private final String text;

    private final String value;

    CodeGenTypeEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value 枚举值
     * @return 用户权限枚举
     */
    public static CodeGenTypeEnum getEnumByValue(String value) {
        if (value == null) {
            return null;
        }
        for (CodeGenTypeEnum codeGenTypeEnum : CodeGenTypeEnum.values()) {
            if (codeGenTypeEnum.value.equals(value)) {
                return codeGenTypeEnum;
            }
        }
        return null;
    }
}
