package com.zhaoxuchun.chat;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

public record ChatMessage(
        MessageType type,
        String sender,
        String target,
        String content,
        LocalDateTime sentAt
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    public static ChatMessage of(
            MessageType type,
            String sender,
            String target,
            String content
    ) {
        return new ChatMessage(type, sender, target, content, LocalDateTime.now());
    }
}
