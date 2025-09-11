package com.j0schi.DreamChat.model;

import com.j0schi.DreamChat.enums.ChatType;
import lombok.Data;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "chats")
public class Chat {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Enumerated(EnumType.STRING)
    private ChatType type;

    private String title;
    private String avatarUrl;

    @ManyToMany
    @JoinTable(
            name = "chat_participants",
            joinColumns = @JoinColumn(name = "chat_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private List<User> participants = new ArrayList<>();

    @OneToOne
    @JoinColumn(name = "last_message_id")
    private Message lastMessage;

    private int unreadCount = 0;
    private LocalDateTime createdAt = LocalDateTime.now();

    // Для приватных чатов
    public boolean isPrivateChat() {
        return type == ChatType.PRIVATE && participants.size() == 2;
    }
}