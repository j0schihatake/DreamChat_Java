package com.j0schi.DreamChat.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.j0schi.DreamChat.enums.MessageStatus;
import com.j0schi.DreamChat.enums.MessageType;
import lombok.*;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Data
public class Message {

    private String id;
    private Chat chat;
    private User sender;
    private String senderName;
    private String content;
    private String fileUrl;
    private String fileName;
    private Long fileSize;

    private MessageType type = MessageType.TEXT;
    private LocalDateTime timestamp = LocalDateTime.now();
    private MessageStatus status = MessageStatus.SENDING;

    public String getSenderId() {
        return sender != null ? sender.getId() : null;
    }

    @JsonIgnore
    public String getChatId() {
        return chat != null ? chat.getId() : null;
    }

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