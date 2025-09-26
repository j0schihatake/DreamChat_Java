package com.j0schi.DreamChat.model;

import com.j0schi.DreamChat.enums.ChatType;
import lombok.*;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class Chat {

    private String id;

    private String title;

    private String avatarUrl;

    private int unreadCount = 0;

    private Message lastMessage;

    private ChatType type;

    private LocalDateTime createdAt = LocalDateTime.now();

    private List<User> participants = new ArrayList<>();

    public boolean isPrivateChat() {
        return type == ChatType.PRIVATE && participants.size() == 2;
    }
}