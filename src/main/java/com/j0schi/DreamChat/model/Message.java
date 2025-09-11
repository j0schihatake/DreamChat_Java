package com.j0schi.DreamChat.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.j0schi.DreamChat.enums.MessageStatus;
import com.j0schi.DreamChat.enums.MessageType;
import lombok.Data;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "messages")
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne
    @JoinColumn(name = "chat_id", nullable = false)
    private Chat chat;

    @ManyToOne
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    private String senderName;
    private String content;

    @Enumerated(EnumType.STRING)
    private MessageType type = MessageType.TEXT;

    private LocalDateTime timestamp = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    private MessageStatus status = MessageStatus.SENDING;

    private String fileUrl;
    private String fileName;
    private Long fileSize;

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
        if (status == null) {
            status = MessageStatus.SENDING;
        }
    }

    // Добавим методы для удобства
    @JsonIgnore
    public String getSenderId() {
        return sender != null ? sender.getId() : null;
    }

    @JsonIgnore
    public String getChatId() {
        return chat != null ? chat.getId() : null;
    }

    // Методы для установки по ID
    public void setSenderId(String senderId) {
        if (senderId != null) {
            User user = new User();
            user.setId(senderId);
            this.sender = user;
        }
    }

    public void setChatId(String chatId) {
        if (chatId != null) {
            Chat chat = new Chat();
            chat.setId(chatId);
            this.chat = chat;
        }
    }
}