package com.j0schi.DreamChat.postgres;

import com.j0schi.DreamChat.model.Message;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class ChatQueryUtil {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Message queries
    public String createMessageTable() {
        return """
            CREATE TABLE IF NOT EXISTS messages (
                id VARCHAR(36) PRIMARY KEY,
                chat_id VARCHAR(36) NOT NULL,
                sender_id VARCHAR(36) NOT NULL,
                sender_name VARCHAR(255),
                content TEXT,
                type VARCHAR(50) NOT NULL,
                timestamp DATETIME NOT NULL,
                status VARCHAR(50) NOT NULL,
                file_url VARCHAR(500),
                file_name VARCHAR(255),
                file_size BIGINT,
                FOREIGN KEY (chat_id) REFERENCES chats(id),
                FOREIGN KEY (sender_id) REFERENCES users(id)
            )
            """;
    }

    public String insertMessageQuery(Message message) {
        return String.format(
                "INSERT INTO messages (id, chat_id, sender_id, sender_name, content, type, timestamp, status, file_url, file_name, file_size) " +
                        "VALUES ('%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', %d)",
                message.getId(), message.getChatId(), message.getSenderId(),
                escapeSql(message.getSenderName()), escapeSql(message.getContent()),
                message.getType(), formatDateTime(message.getTimestamp()), message.getStatus(),
                escapeSql(message.getFileUrl()), escapeSql(message.getFileName()),
                message.getFileSize() != null ? message.getFileSize() : 0
        );
    }

    public String updateMessageQuery(Message message) {
        return String.format(
                "UPDATE messages SET content = '%s', status = '%s', file_url = '%s', file_name = '%s', file_size = %d " +
                        "WHERE id = '%s'",
                escapeSql(message.getContent()), message.getStatus(),
                escapeSql(message.getFileUrl()), escapeSql(message.getFileName()),
                message.getFileSize() != null ? message.getFileSize() : 0, message.getId()
        );
    }

    public String selectMessageByIdQuery(String id) {
        return "SELECT * FROM messages WHERE id = '" + id + "'";
    }

    public String selectMessagesByChatIdQuery(String chatId, int limit, int offset) {
        return String.format(
                "SELECT * FROM messages WHERE chat_id = '%s' ORDER BY timestamp DESC LIMIT %d OFFSET %d",
                chatId, limit, offset
        );
    }

    public String selectLastMessageByChatIdQuery(String chatId) {
        return String.format(
                "SELECT * FROM messages WHERE chat_id = '%s' ORDER BY timestamp DESC LIMIT 1",
                chatId
        );
    }

    // Chat queries
    public String createChatTable() {
        return """
            CREATE TABLE IF NOT EXISTS chats (
                id VARCHAR(36) PRIMARY KEY,
                type VARCHAR(50) NOT NULL,
                title VARCHAR(255),
                avatar_url VARCHAR(500),
                last_message_id VARCHAR(36),
                unread_count INT DEFAULT 0,
                created_at DATETIME NOT NULL,
                FOREIGN KEY (last_message_id) REFERENCES messages(id)
            )
            """;
    }

    public String createChatParticipantsTable() {
        return """
            CREATE TABLE IF NOT EXISTS chat_participants (
                chat_id VARCHAR(36) NOT NULL,
                user_id VARCHAR(36) NOT NULL,
                PRIMARY KEY (chat_id, user_id),
                FOREIGN KEY (chat_id) REFERENCES chats(id),
                FOREIGN KEY (user_id) REFERENCES users(id)
            )
            """;
    }

    // User queries
    public String createUserTable() {
        return """
            CREATE TABLE IF NOT EXISTS users (
                id VARCHAR(36) PRIMARY KEY,
                phone_number VARCHAR(20) UNIQUE NOT NULL,
                username VARCHAR(255) UNIQUE,
                email VARCHAR(255),
                avatar_url VARCHAR(500),
                status VARCHAR(50) DEFAULT 'OFFLINE',
                last_seen DATETIME,
                created_at DATETIME NOT NULL,
                device_id VARCHAR(255),
                is_authorized BOOLEAN DEFAULT FALSE
            )
            """;
    }

    private String escapeSql(String value) {
        return value != null ? value.replace("'", "''") : "";
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(formatter) : LocalDateTime.now().format(formatter);
    }
}