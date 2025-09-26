package com.j0schi.DreamChat.postgres.mapper;

import com.j0schi.DreamChat.model.Message;
import com.j0schi.DreamChat.enums.MessageStatus;
import com.j0schi.DreamChat.enums.MessageType;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

@Component
public class MessageMapper implements RowMapper<Message> {

    @Override
    public Message mapRow(ResultSet rs, int rowNum) throws SQLException {
        Message message = new Message();
        message.setId(rs.getString("id"));
        message.setContent(rs.getString("content"));
        message.setSenderName(rs.getString("sender_name"));
        message.setType(MessageType.valueOf(rs.getString("type")));
        message.setStatus(MessageStatus.valueOf(rs.getString("status")));
        message.setTimestamp(rs.getObject("timestamp", LocalDateTime.class));
        message.setFileUrl(rs.getString("file_url"));
        message.setFileName(rs.getString("file_name"));
        message.setFileSize(rs.getLong("file_size"));

        // Устанавливаем только ID для связей
        message.setChatId(rs.getString("chat_id"));
        message.setSenderId(rs.getString("sender_id"));

        return message;
    }
}