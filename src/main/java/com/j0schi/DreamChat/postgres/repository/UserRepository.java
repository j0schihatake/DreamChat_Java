package com.j0schi.DreamChat.postgres.repository;

import com.j0schi.DreamChat.postgres.mapper.UserMapper;
import com.j0schi.DreamChat.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserRepository {

    private final JdbcTemplate jdbcTemplate;
    private final UserMapper userMapper;

    public Boolean execute(String query) {
        try {
            jdbcTemplate.execute(query);
            return true;
        } catch (Exception ex) {
            System.err.println("Query error: " + query + " - " + ex.getMessage());
            return false;
        }
    }

    public User getUser(String query) {
        try {
            return jdbcTemplate.queryForObject(query, userMapper);
        } catch (Exception ex) {
            System.err.println("Query error: " + query + " - " + ex.getMessage());
            return null;
        }
    }

    public List<User> getUsers(String query) {
        try {
            return jdbcTemplate.query(query, userMapper);
        } catch (Exception ex) {
            System.err.println("Query error: " + query + " - " + ex.getMessage());
            return null;
        }
    }

    // Основные CRUD операции
    public User findById(String id) {
        return getUser("SELECT * FROM users WHERE id = '" + id + "'");
    }

    public List<User> findAll() {
        return getUsers("SELECT * FROM users ORDER BY created_at DESC");
    }

    public boolean save(User user) {
        User existing = findById(user.getId());
        String query = existing != null ? generateUpdateQuery(user) : generateInsertQuery(user);
        return execute(query);
    }

    public boolean deleteById(String id) {
        return execute("DELETE FROM users WHERE id = '" + id + "'");
    }

    // Кастомные методы из JPA репозитория
    public Optional<User> findByPhoneNumber(String phoneNumber) {
        User user = getUser("SELECT * FROM users WHERE phone_number = '" + phoneNumber + "'");
        return Optional.ofNullable(user);
    }

    public Optional<User> findByDeviceId(String deviceId) {
        User user = getUser("SELECT * FROM users WHERE device_id = '" + deviceId + "'");
        return Optional.ofNullable(user);
    }

    public boolean existsByPhoneNumber(String phoneNumber) {
        try {
            String query = "SELECT COUNT(*) FROM users WHERE phone_number = '" + phoneNumber + "'";
            Integer count = jdbcTemplate.queryForObject(query, Integer.class);
            return count != null && count > 0;
        } catch (Exception ex) {
            System.err.println("Exists check error: " + ex.getMessage());
            return false;
        }
    }

    // Дополнительные полезные методы
    public Optional<User> findByUsername(String username) {
        User user = getUser("SELECT * FROM users WHERE username = '" + username + "'");
        return Optional.ofNullable(user);
    }

    public Optional<User> findByEmail(String email) {
        User user = getUser("SELECT * FROM users WHERE email = '" + email + "'");
        return Optional.ofNullable(user);
    }

    public List<User> findByStatus(String status) {
        return getUsers("SELECT * FROM users WHERE status = '" + status + "'");
    }

    public boolean updateUserStatus(String userId, String status) {
        return execute(
                "UPDATE users SET status = '" + status + "' WHERE id = '" + userId + "'"
        );
    }

    public boolean updateLastSeen(String userId, java.time.LocalDateTime lastSeen) {
        return execute(
                "UPDATE users SET last_seen = '" + formatDateTime(lastSeen) + "' WHERE id = '" + userId + "'"
        );
    }

    public boolean updateAuthorizationStatus(String userId, boolean isAuthorized) {
        return execute(
                "UPDATE users SET is_authorized = " + isAuthorized + " WHERE id = '" + userId + "'"
        );
    }

    // Вспомогательные методы для генерации запросов
    private String generateInsertQuery(User user) {
        return String.format(
                "INSERT INTO users (id, phone_number, username, email, avatar_url, status, last_seen, created_at, device_id, is_authorized) " +
                        "VALUES ('%s', '%s', '%s', '%s', '%s', '%s', %s, '%s', '%s', %b)",
                user.getId(), escapeSql(user.getPhoneNumber()), escapeSql(user.getUsername()),
                escapeSql(user.getEmail()), escapeSql(user.getAvatarUrl()), user.getStatus(),
                user.getLastSeen() != null ? "'" + formatDateTime(user.getLastSeen()) + "'" : "NULL",
                formatDateTime(user.getCreatedAt()), escapeSql(user.getDeviceId()), user.isAuthorized()
        );
    }

    private String generateUpdateQuery(User user) {
        return String.format(
                "UPDATE users SET phone_number = '%s', username = '%s', email = '%s', " +
                        "avatar_url = '%s', status = '%s', last_seen = %s, device_id = '%s', is_authorized = %b " +
                        "WHERE id = '%s'",
                escapeSql(user.getPhoneNumber()), escapeSql(user.getUsername()), escapeSql(user.getEmail()),
                escapeSql(user.getAvatarUrl()), user.getStatus(),
                user.getLastSeen() != null ? "'" + formatDateTime(user.getLastSeen()) + "'" : "NULL",
                escapeSql(user.getDeviceId()), user.isAuthorized(), user.getId()
        );
    }

    private String escapeSql(String value) {
        return value != null ? value.replace("'", "''") : "";
    }

    private String formatDateTime(java.time.LocalDateTime dateTime) {
        if (dateTime == null) return "NULL";
        return dateTime.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    /**
     * Новый метод для поиска пользователей по частичному совпадению имени
     */
    public List<User> findByUsernameContaining(String username) {
        return getUsers(
                "SELECT * FROM users WHERE username LIKE '%" + escapeSql(username) + "%' " +
                        "ORDER BY username"
        );
    }

    /**
     * Метод для получения пользователей, созданных после указанной даты
     */
    public List<User> findByCreatedAtAfter(java.time.LocalDateTime date) {
        return getUsers(
                "SELECT * FROM users WHERE created_at > '" + formatDateTime(date) + "' " +
                        "ORDER BY created_at DESC"
        );
    }

    /**
     * Метод для получения пользователей с последним посещением после указанной даты
     */
    public List<User> findByLastSeenAfter(java.time.LocalDateTime date) {
        return getUsers(
                "SELECT * FROM users WHERE last_seen > '" + formatDateTime(date) + "' " +
                        "ORDER BY last_seen DESC"
        );
    }

    /**
     * Метод для получения количества пользователей
     */
    public long count() {
        try {
            String query = "SELECT COUNT(*) FROM users";
            return jdbcTemplate.queryForObject(query, Long.class);
        } catch (Exception ex) {
            System.err.println("Count error: " + ex.getMessage());
            return 0;
        }
    }

    /**
     * Метод для получения пользователей с пагинацией
     */
    public List<User> findAllWithPagination(int offset, int limit) {
        return getUsers(
                "SELECT * FROM users ORDER BY created_at DESC LIMIT " + limit + " OFFSET " + offset
        );
    }

    /**
     * Метод для обновления аватара пользователя
     */
    public boolean updateAvatar(String userId, String avatarUrl) {
        return execute(
                "UPDATE users SET avatar_url = '" + escapeSql(avatarUrl) + "' WHERE id = '" + userId + "'"
        );
    }

    /**
     * Метод для обновления имени пользователя
     */
    public boolean updateUsername(String userId, String username) {
        return execute(
                "UPDATE users SET username = '" + escapeSql(username) + "' WHERE id = '" + userId + "'"
        );
    }

    /**
     * Метод для обновления email пользователя
     */
    public boolean updateEmail(String userId, String email) {
        return execute(
                "UPDATE users SET email = '" + escapeSql(email) + "' WHERE id = '" + userId + "'"
        );
    }

    /**
     * Метод для проверки существования username
     */
    public boolean existsByUsername(String username) {
        try {
            String query = "SELECT COUNT(*) FROM users WHERE username = '" + escapeSql(username) + "'";
            Integer count = jdbcTemplate.queryForObject(query, Integer.class);
            return count != null && count > 0;
        } catch (Exception ex) {
            System.err.println("Exists check error: " + ex.getMessage());
            return false;
        }
    }

    /**
     * Метод для проверки существования email
     */
    public boolean existsByEmail(String email) {
        try {
            String query = "SELECT COUNT(*) FROM users WHERE email = '" + escapeSql(email) + "'";
            Integer count = jdbcTemplate.queryForObject(query, Integer.class);
            return count != null && count > 0;
        } catch (Exception ex) {
            System.err.println("Exists check error: " + ex.getMessage());
            return false;
        }
    }
}