package com.j0schi.DreamChat.postgres.repository;

import com.j0schi.DreamChat.postgres.mapper.ChatMapper;
import com.j0schi.DreamChat.model.Chat;
import com.j0schi.DreamChat.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ChatRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ChatMapper chatMapper;
    private final UserRepository userRepository;

    public Boolean execute(String query) {
        try {
            jdbcTemplate.execute(query);
            return true;
        } catch (Exception ex) {
            System.err.println("Query error: " + query + " - " + ex.getMessage());
            return false;
        }
    }

    public Chat getChat(String query) {
        try {
            return jdbcTemplate.queryForObject(query, chatMapper);
        } catch (Exception ex) {
            System.err.println("Query error: " + query + " - " + ex.getMessage());
            return null;
        }
    }

    public List<Chat> getChats(String query) {
        try {
            return jdbcTemplate.query(query, chatMapper);
        } catch (Exception ex) {
            System.err.println("Query error: " + query + " - " + ex.getMessage());
            return null;
        }
    }

    // Основные CRUD операции
    public Chat findById(String id) {
        return getChat("SELECT * FROM chats WHERE id = '" + id + "'");
    }

    public List<Chat> findAll() {
        return getChats("SELECT * FROM chats ORDER BY created_at DESC");
    }

    public boolean save(Chat chat) {
        Chat existing = findById(chat.getId());
        String query = existing != null ? generateUpdateQuery(chat) : generateInsertQuery(chat);
        return execute(query);
    }

    public boolean deleteById(String id) {
        // Сначала удаляем связи участников
        execute("DELETE FROM chat_participants WHERE chat_id = '" + id + "'");
        return execute("DELETE FROM chats WHERE id = '" + id + "'");
    }

    // Кастомные методы из JPA репозитория
    public List<Chat> findByParticipantsContaining(User user) {
        return getChats(
                "SELECT c.* FROM chats c " +
                        "JOIN chat_participants cp ON c.id = cp.chat_id " +
                        "WHERE cp.user_id = '" + user.getId() + "' " +
                        "ORDER BY c.created_at DESC"
        );
    }

    public Optional<Chat> findPrivateChatBetweenUsers(String user1Id, String user2Id) {
        String query =
                "SELECT c.* FROM chats c " +
                        "WHERE c.type = 'PRIVATE' AND " +
                        "EXISTS (SELECT 1 FROM chat_participants cp WHERE cp.chat_id = c.id AND cp.user_id = '" + user1Id + "') AND " +
                        "EXISTS (SELECT 1 FROM chat_participants cp WHERE cp.chat_id = c.id AND cp.user_id = '" + user2Id + "') AND " +
                        "(SELECT COUNT(*) FROM chat_participants cp WHERE cp.chat_id = c.id) = 2 " +
                        "LIMIT 1";

        Chat chat = getChat(query);
        return Optional.ofNullable(chat);
    }

    public Optional<Chat> findByTitleAndUser(String title, String userId) {
        String query =
                "SELECT c.* FROM chats c " +
                        "JOIN chat_participants cp ON c.id = cp.chat_id " +
                        "WHERE c.title = '" + title + "' AND cp.user_id = '" + userId + "' " +
                        "LIMIT 1";

        Chat chat = getChat(query);
        return Optional.ofNullable(chat);
    }

    public List<Chat> findByUserId(String userId) {
        return getChats(
                "SELECT DISTINCT c.* FROM chats c " +
                        "JOIN chat_participants cp ON c.id = cp.chat_id " +
                        "WHERE cp.user_id = '" + userId + "' " +
                        "ORDER BY c.created_at DESC"
        );
    }

    public List<Chat> findByUserIdOrderByLastMessageTimestampDesc(String userId) {
        return getChats(
                "SELECT c.* FROM chats c " +
                        "JOIN chat_participants cp ON c.id = cp.chat_id " +
                        "LEFT JOIN messages m ON c.last_message_id = m.id " +
                        "WHERE cp.user_id = '" + userId + "' " +
                        "ORDER BY COALESCE(m.timestamp, c.created_at) DESC"
        );
    }

    public Optional<Chat> findPrivateChatByUserIds(String user1Id, String user2Id) {
        return findPrivateChatBetweenUsers(user1Id, user2Id);
    }

    // Методы для работы с участниками чата
    public boolean addParticipantToChat(String chatId, String userId) {
        return execute(
                "INSERT INTO chat_participants (chat_id, user_id) VALUES ('" + chatId + "', '" + userId + "')"
        );
    }

    public boolean removeParticipantFromChat(String chatId, String userId) {
        return execute(
                "DELETE FROM chat_participants WHERE chat_id = '" + chatId + "' AND user_id = '" + userId + "'"
        );
    }

    public List<User> getChatParticipants(String chatId) {
        String query =
                "SELECT u.* FROM users u " +
                        "JOIN chat_participants cp ON u.id = cp.user_id " +
                        "WHERE cp.chat_id = '" + chatId + "'";

        return userRepository.getUsers(query);
    }

    public boolean updateLastMessage(String chatId, String lastMessageId) {
        return execute(
                "UPDATE chats SET last_message_id = '" + lastMessageId + "' WHERE id = '" + chatId + "'"
        );
    }

    // Вспомогательные методы для генерации запросов
    private String generateInsertQuery(Chat chat) {
        return String.format(
                "INSERT INTO chats (id, type, title, avatar_url, last_message_id, unread_count, created_at) " +
                        "VALUES ('%s', '%s', '%s', '%s', %s, %d, '%s')",
                chat.getId(), chat.getType(), escapeSql(chat.getTitle()),
                escapeSql(chat.getAvatarUrl()),
                chat.getLastMessage() != null ? "'" + chat.getLastMessage().getId() + "'" : "NULL",
                chat.getUnreadCount(), formatDateTime(chat.getCreatedAt())
        );
    }

    private String generateUpdateQuery(Chat chat) {
        return String.format(
                "UPDATE chats SET type = '%s', title = '%s', avatar_url = '%s', " +
                        "last_message_id = %s, unread_count = %d " +
                        "WHERE id = '%s'",
                chat.getType(), escapeSql(chat.getTitle()), escapeSql(chat.getAvatarUrl()),
                chat.getLastMessage() != null ? "'" + chat.getLastMessage().getId() + "'" : "NULL",
                chat.getUnreadCount(), chat.getId()
        );
    }

    private String escapeSql(String value) {
        return value != null ? value.replace("'", "''") : "";
    }

    private String formatDateTime(java.time.LocalDateTime dateTime) {
        if (dateTime == null) return "NULL";
        return dateTime.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}