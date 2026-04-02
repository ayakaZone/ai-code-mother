package com.neko.nekoaicodemother.ai.model.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StreamMessage {
    /**
     * AI 响应消息的类型
     */
    private String type;
}
