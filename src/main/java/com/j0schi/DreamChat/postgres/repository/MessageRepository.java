package com.j0schi.DreamChat.postgres.repository;

import com.j0schi.DreamChat.postgres.mapper.MessageMapper;
import com.j0schi.DreamChat.model.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class MessageRepository {

    private final JdbcTemplate jdbcTemplate;
    private final MessageMapper messageMapper;

    public Boolean execute(String query) {
        try {
            jdbcTemplate.execute(query);
            return true;
        } catch (Exception ex) {
            System.err.println("Query error: " + query + " - " + ex.getMessage());
            return false;
        }
    }

    public Message getMessage(String query) {
        try {
            return jdbcTemplate.queryForObject(query, messageMapper);
        } catch (Exception ex) {
            System.err.println("Query error: " + query + " - " + ex.getMessage());
            return null;
        }
    }

    public List<Message> getMessages(String query) {
        try {
            return jdbcTemplate.query(query, messageMapper);
        } catch (Exception ex) {
            System.err.println("Query error: " + query + " - " + ex.getMessage());
            return null;
        }
    }

    // Основные CRUD операции
    public Message findById(String id) {
        return getMessage("SELECT * FROM messages WHERE id = '" + id + "'");
    }

    public List<Message> findAll() {
        return getMessages("SELECT * FROM messages ORDER BY timestamp DESC");
    }

    public boolean save(Message message) {
        Message existing = findById(message.getId());
        String query = existing != null ? generateUpdateQuery(message) : generateInsertQuery(message);
        return execute(query);
    }

    public boolean deleteById(String id) {
        return execute("DELETE FROM messages WHERE id = '" + id + "'");
    }

    // Кастомные методы из JPA репозитория
    public List<Message> findByChatIdOrderByTimestampDesc(String chatId, int limit, int offset) {
        return getMessages(
                "SELECT * FROM messages WHERE chat_id = '" + chatId + "' " +
                        "ORDER BY timestamp DESC LIMIT " + limit + " OFFSET " + offset
        );
    }

    public List<Message> findLatestMessages(String chatId, int limit) {
        return getMessages(
                "SELECT * FROM messages WHERE chat_id = '" + chatId + "' " +
                        "ORDER BY timestamp DESC LIMIT " + limit
        );
    }

    public long countUnreadMessages(String chatId, String senderId) {
        try {
            String query = String.format(
                    "SELECT COUNT(*) FROM messages WHERE chat_id = '%s' " +
                            "AND status != 'READ' AND sender_id != '%s'",
                    chatId, senderId
            );
            return jdbcTemplate.queryForObject(query, Long.class);
        } catch (Exception ex) {
            System.err.println("Count error: " + ex.getMessage());
            return 0;
        }
    }

    // Дополнительные полезные методы
    public List<Message> findBySenderId(String senderId) {
        return getMessages(
                "SELECT * FROM messages WHERE sender_id = '" + senderId + "' " +
                        "ORDER BY timestamp DESC"
        );
    }

    public List<Message> findByStatus(String status) {
        return getMessages(
                "SELECT * FROM messages WHERE status = '" + status + "' " +
                        "ORDER BY timestamp DESC"
        );
    }

    public boolean updateMessageStatus(String messageId, String status) {
        return execute(
                "UPDATE messages SET status = '" + status + "' WHERE id = '" + messageId + "'"
        );
    }

    public boolean updateMessageStatusBatch(List<String> messageIds, String status) {
        if (messageIds == null || messageIds.isEmpty()) return true;

        String ids = String.join("','", messageIds);
        return execute(
                "UPDATE messages SET status = '" + status + "' WHERE id IN ('" + ids + "')"
        );
    }

    public Message findLastMessageByChatId(String chatId) {
        List<Message> messages = getMessages(
                "SELECT * FROM messages WHERE chat_id = '" + chatId + "' " +
                        "ORDER BY timestamp DESC LIMIT 1"
        );
        return messages != null && !messages.isEmpty() ? messages.get(0) : null;
    }

    // Вспомогательные методы для генерации запросов
    private String generateInsertQuery(Message message) {
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

    private String generateUpdateQuery(Message message) {
        return String.format(
                "UPDATE messages SET content = '%s', status = '%s', file_url = '%s', " +
                        "file_name = '%s', file_size = %d WHERE id = '%s'",
                escapeSql(message.getContent()), message.getStatus(),
                escapeSql(message.getFileUrl()), escapeSql(message.getFileName()),
                message.getFileSize() != null ? message.getFileSize() : 0, message.getId()
        );
    }

    private String escapeSql(String value) {
        return value != null ? value.replace("'", "''") : "";
    }

    private String formatDateTime(java.time.LocalDateTime dateTime) {
        if (dateTime == null) return "NULL";
        return dateTime.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public List<Message> findByChatIdOrderByTimestampDesc(String chatId, Pageable pageable) {
        try {
            String query = "SELECT * FROM messages WHERE chat_id = ? ORDER BY timestamp DESC LIMIT ? OFFSET ?";
            return jdbcTemplate.query(query, messageMapper, chatId, pageable.getPageSize(), pageable.getOffset());
        } catch (Exception ex) {
            System.err.println("Query error for chatId: " + chatId + " - " + ex.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Новый метод для пагинации с указанием лимита и оффсета
     */
    public List<Message> findByChatIdWithPagination(String chatId, int offset, int limit) {
        return getMessages(
                "SELECT * FROM messages WHERE chat_id = '" + chatId + "' " +
                        "ORDER BY timestamp DESC LIMIT " + limit + " OFFSET " + offset
        );
    }

    /**
     * Метод для получения сообщений по chatId без пагинации
     */
    public List<Message> findByChatId(String chatId) {
        return getMessages(
                "SELECT * FROM messages WHERE chat_id = '" + chatId + "' " +
                        "ORDER BY timestamp DESC"
        );
    }

    /**
     * Метод для получения сообщений по chatId с лимитом и оффсетом (альтернативное название)
     */
    public List<Message> findByChatId(String chatId, int limit, int offset) {
        return findByChatIdWithPagination(chatId, offset, limit);
    }

    /**
     * Метод для подсчета общего количества сообщений в чате
     */
    public long countByChatId(String chatId) {
        try {
            String query = "SELECT COUNT(*) FROM messages WHERE chat_id = '" + chatId + "'";
            return jdbcTemplate.queryForObject(query, Long.class);
        } catch (Exception ex) {
            System.err.println("Count error: " + ex.getMessage());
            return 0;
        }
    }

    /**
     * Метод для получения сообщений по диапазону дат
     */
    public List<Message> findByChatIdAndTimestampBetween(String chatId,
                                                         java.time.LocalDateTime startDate,
                                                         java.time.LocalDateTime endDate) {
        return getMessages(
                "SELECT * FROM messages WHERE chat_id = '" + chatId + "' " +
                        "AND timestamp BETWEEN '" + formatDateTime(startDate) + "' " +
                        "AND '" + formatDateTime(endDate) + "' " +
                        "ORDER BY timestamp DESC"
        );
    }

    /**
     * Метод для поиска сообщений по содержанию в конкретном чате
     */
    public List<Message> findByChatIdAndContentContaining(String chatId, String content) {
        return getMessages(
                "SELECT * FROM messages WHERE chat_id = '" + chatId + "' " +
                        "AND content LIKE '%" + escapeSql(content) + "%' " +
                        "ORDER BY timestamp DESC"
        );
    }
}