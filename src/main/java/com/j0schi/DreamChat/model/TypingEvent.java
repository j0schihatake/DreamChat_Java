package com.j0schi.DreamChat.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TypingEvent {
    private String chatId;
    private String userId;
    private String username;
    private boolean typing; // true - начал печатать, false - закончил
    private LocalDateTime timestamp;

    public TypingEvent() {
        this.timestamp = LocalDateTime.now();
    }

    public TypingEvent(String chatId, String userId, String username, boolean typing) {
        this.chatId = chatId;
        this.userId = userId;
        this.username = username;
        this.typing = typing;
        this.timestamp = LocalDateTime.now();
    }
}