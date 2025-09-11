package com.j0schi.DreamChat.dto;

import com.j0schi.DreamChat.enums.MessageStatus;
import com.j0schi.DreamChat.enums.MessageType;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class MessageDTO {
    private String id;
    private String chatId;
    private String senderId;
    private String senderName;
    private String content;
    private MessageType type;
    private LocalDateTime timestamp;
    private MessageStatus status;
    private String fileUrl;
    private String fileName;
    private Long fileSize;
}