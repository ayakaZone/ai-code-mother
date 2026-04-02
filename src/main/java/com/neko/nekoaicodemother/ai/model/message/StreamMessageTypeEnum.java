package com.neko.nekoaicodemother.ai.model.message;

import lombok.Getter;

@Getter
public enum StreamMessageTypeEnum {

    /**
     * AI 响应消息类型枚举类
     */

    AI_RESPONSE("ai_response", "AI响应"),
    TOOL_REQUEST("tool_request", "工具调用请求"),
    TOOL_EXECUTED("tool_executed", "工具执行结果");

    private final String text;
    private final String value;

    StreamMessageTypeEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    public static StreamMessageTypeEnum getEnumByValue(String value) {
        if(value == null) {
            return null;
        }
        for (StreamMessageTypeEnum item : values()) {
            if (item.value.equals(value)) {
                return item;
            }
        }
        return null;
    }
}
