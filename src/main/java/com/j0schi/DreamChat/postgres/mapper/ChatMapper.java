package com.j0schi.DreamChat.postgres.mapper;

import com.j0schi.DreamChat.model.Chat;
import com.j0schi.DreamChat.enums.ChatType;
import com.j0schi.DreamChat.model.Message;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

@Component
public class ChatMapper implements RowMapper<Chat> {

    @Override
    public Chat mapRow(ResultSet rs, int rowNum) throws SQLException {
        Chat chat = new Chat();
        chat.setId(rs.getString("id"));
        chat.setType(ChatType.valueOf(rs.getString("type")));
        chat.setTitle(rs.getString("title"));
        chat.setAvatarUrl(rs.getString("avatar_url"));
        chat.setUnreadCount(rs.getInt("unread_count"));
        chat.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));

        // Last message ID устанавливается отдельно
        String lastMessageId = rs.getString("last_message_id");
        if (lastMessageId != null) {
            // Создаем минимальный объект Message только с ID
            Message lastMessage = new Message();
            lastMessage.setId(lastMessageId);
            chat.setLastMessage(lastMessage);
        }

        return chat;
    }
}