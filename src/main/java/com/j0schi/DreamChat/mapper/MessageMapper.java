package com.j0schi.DreamChat.mapper;

import com.j0schi.DreamChat.dto.MessageDTO;
import com.j0schi.DreamChat.model.Message;
import org.springframework.stereotype.Component;

@Component
public class MessageMapper {

    public MessageDTO toDTO(Message message) {
        MessageDTO dto = new MessageDTO();
        dto.setId(message.getId());
        dto.setChatId(message.getChatId());
        dto.setSenderId(message.getSenderId());
        dto.setSenderName(message.getSenderName());
        dto.setContent(message.getContent());
        dto.setType(message.getType());
        dto.setTimestamp(message.getTimestamp());
        dto.setStatus(message.getStatus());
        dto.setFileUrl(message.getFileUrl());
        dto.setFileName(message.getFileName());
        dto.setFileSize(message.getFileSize());
        return dto;
    }

    public Message toEntity(MessageDTO dto) {
        Message message = new Message();
        message.setId(dto.getId());
        message.setChatId(dto.getChatId());
        message.setSenderId(dto.getSenderId());
        message.setSenderName(dto.getSenderName());
        message.setContent(dto.getContent());
        message.setType(dto.getType());
        message.setTimestamp(dto.getTimestamp());
        message.setStatus(dto.getStatus());
        message.setFileUrl(dto.getFileUrl());
        message.setFileName(dto.getFileName());
        message.setFileSize(dto.getFileSize());
        return message;
    }
}