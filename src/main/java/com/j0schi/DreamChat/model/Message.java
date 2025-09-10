package com.j0schi.DreamChat.model;

import com.j0schi.DreamChat.enums.MessageStatus;
import com.j0schi.DreamChat.enums.MessageType;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;

@Data
public class Message {
    private String id;
    private String chatId;
    private String senderId;
    private String senderName;
    private String content;
    private MessageType type;
    private LocalDateTime timestamp;
    private MessageStatus status;
    private Map<String, Object> metadata; // для дополнительных данных

    // Для файлов/медиа
    private String fileUrl;
    private String fileName;
    private Long fileSize;

    // Для ответов на сообщения
    private String replyToMessageId;
    private Message repliedMessage;

    public Message() {
        this.timestamp = LocalDateTime.now();
        this.status = MessageStatus.SENDING;
        this.type = MessageType.TEXT;
    }
}