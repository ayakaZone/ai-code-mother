package com.neko.nekoaicodemother.model.enums;

import lombok.Getter;

@Getter
public enum ChatHistoryMessageTypeEnum {

    USER("用户", "user"),
    AI("AI", "ai");

    private final String text;
    private final String value;

    ChatHistoryMessageTypeEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据值获取枚举
     *
     * @param value 值
     * @return 枚举
     */
    public static ChatHistoryMessageTypeEnum getEnumByValue(String value) {
        if (value == null) {
            return null;
        }
        for (ChatHistoryMessageTypeEnum type : ChatHistoryMessageTypeEnum.values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        return null;
    }
}
