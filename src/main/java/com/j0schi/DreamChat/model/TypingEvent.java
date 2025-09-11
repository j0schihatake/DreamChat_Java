package com.j0schi.DreamChat.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TypingEvent {
    private String chatId;
    private String userId;
    private String username;
    private boolean typing;
    private LocalDateTime timestamp = LocalDateTime.now();
}