package com.j0schi.DreamChat.service;

import com.j0schi.DreamChat.enums.UserStatus;
import com.j0schi.DreamChat.model.User;
import com.j0schi.DreamChat.postgres.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;

    /**
     * Создание нового пользователя
     */
    public User createUser(User user) {
        try {
            log.info("Creating new user with phone: {}", user.getPhoneNumber());

            // Генерируем ID если не установлен
            if (user.getId() == null) {
                user.setId(UUID.randomUUID().toString());
            }

            // Устанавливаем дату создания если не установлена
            if (user.getCreatedAt() == null) {
                user.setCreatedAt(LocalDateTime.now());
            }

            // Устанавливаем статус по умолчанию
            if (user.getStatus() == null) {
                user.setStatus(UserStatus.valueOf("ACTIVE"));
            }

            // Проверяем, существует ли пользователь с таким номером телефона
            if (userRepository.existsByPhoneNumber(user.getPhoneNumber())) {
                throw new RuntimeException("User with this phone number already exists");
            }

            // Сохраняем пользователя
            boolean saved = userRepository.save(user);
            if (!saved) {
                throw new RuntimeException("Failed to save user");
            }

            log.info("User created successfully: {}", user.getId());
            return user;

        } catch (Exception e) {
            log.error("Error creating user: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create user", e);
        }
    }

    /**
     * Обновление пользователя
     */
    public User updateUser(String userId, User user) {
        try {
            log.info("Updating user: {}", userId);

            // Проверяем существование пользователя
            User existingUser = userRepository.findById(userId);
            if (existingUser == null) {
                throw new RuntimeException("User not found");
            }

            // Обновляем разрешенные поля
            if (user.getUsername() != null) {
                existingUser.setUsername(user.getUsername());
            }
            if (user.getEmail() != null) {
                existingUser.setEmail(user.getEmail());
            }
            if (user.getAvatarUrl() != null) {
                existingUser.setAvatarUrl(user.getAvatarUrl());
            }
            if (user.getStatus() != null) {
                existingUser.setStatus(user.getStatus());
            }
            if (user.getDeviceId() != null) {
                existingUser.setDeviceId(user.getDeviceId());
            }

            // Сохраняем изменения
            boolean updated = userRepository.save(existingUser);
            if (!updated) {
                throw new RuntimeException("Failed to update user");
            }

            log.info("User updated successfully: {}", userId);
            return existingUser;

        } catch (Exception e) {
            log.error("Error updating user: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update user", e);
        }
    }

    /**
     * Обновление пользователя (перегруженный метод)
     */
    public User updateUser(User user) {
        if (user.getId() == null) {
            throw new RuntimeException("User ID cannot be null");
        }
        return updateUser(user.getId(), user);
    }

    /**
     * Получение пользователя по ID
     */
    public User getUserById(String userId) {
        try {
            User user = userRepository.findById(userId);
            if (user == null) {
                throw new RuntimeException("User not found");
            }
            return user;
        } catch (Exception e) {
            log.error("Error getting user by id: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get user by id", e);
        }
    }

    /**
     * Поиск пользователя по номеру телефона
     */
    public Optional<User> findByPhoneNumber(String phoneNumber) {
        try {
            return userRepository.findByPhoneNumber(phoneNumber);
        } catch (Exception e) {
            log.error("Error finding user by phone: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Поиск пользователя по имени пользователя
     */
    public Optional<User> findByUsername(String username) {
        try {
            return userRepository.findByUsername(username);
        } catch (Exception e) {
            log.error("Error finding user by username: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Поиск пользователя по email
     */
    public Optional<User> findByEmail(String email) {
        try {
            return userRepository.findByEmail(email);
        } catch (Exception e) {
            log.error("Error finding user by email: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Получение всех пользователей
     */
    public List<User> getAllUsers() {
        try {
            return userRepository.findAll();
        } catch (Exception e) {
            log.error("Error getting all users: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get all users", e);
        }
    }

    /**
     * Получение пользователей по статусу
     */
    public List<User> getUsersByStatus(String status) {
        try {
            return userRepository.findByStatus(status);
        } catch (Exception e) {
            log.error("Error getting users by status: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get users by status", e);
        }
    }

    /**
     * Удаление пользователя
     */
    public boolean deleteUser(String userId) {
        try {
            log.info("Deleting user: {}", userId);

            boolean deleted = userRepository.deleteById(userId);
            if (deleted) {
                log.info("User deleted successfully: {}", userId);
            } else {
                log.warn("User not found or already deleted: {}", userId);
            }

            return deleted;

        } catch (Exception e) {
            log.error("Error deleting user: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to delete user", e);
        }
    }

    /**
     * Обновление статуса пользователя
     */
    public boolean updateUserStatus(String userId, String status) {
        try {
            log.info("Updating user status: {} -> {}", userId, status);

            boolean updated = userRepository.updateUserStatus(userId, status);
            if (updated) {
                log.info("User status updated successfully: {}", userId);
            }

            return updated;

        } catch (Exception e) {
            log.error("Error updating user status: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update user status", e);
        }
    }

    /**
     * Обновление времени последнего посещения
     */
    public boolean updateLastSeen(String userId, LocalDateTime lastSeen) {
        try {
            log.info("Updating last seen for user: {} -> {}", userId, lastSeen);

            boolean updated = userRepository.updateLastSeen(userId, lastSeen);
            if (updated) {
                log.info("Last seen updated successfully: {}", userId);
            }

            return updated;

        } catch (Exception e) {
            log.error("Error updating last seen: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update last seen", e);
        }
    }

    /**
     * Обновление статуса авторизации
     */
    public boolean updateAuthorizationStatus(String userId, boolean isAuthorized) {
        try {
            log.info("Updating authorization status for user: {} -> {}", userId, isAuthorized);

            boolean updated = userRepository.updateAuthorizationStatus(userId, isAuthorized);
            if (updated) {
                log.info("Authorization status updated successfully: {}", userId);
            }

            return updated;

        } catch (Exception e) {
            log.error("Error updating authorization status: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update authorization status", e);
        }
    }

    /**
     * Проверка существования пользователя по номеру телефона
     */
    public boolean existsByPhoneNumber(String phoneNumber) {
        try {
            return userRepository.existsByPhoneNumber(phoneNumber);
        } catch (Exception e) {
            log.error("Error checking user existence: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Поиск пользователя по deviceId
     */
    public Optional<User> findByDeviceId(String deviceId) {
        try {
            return userRepository.findByDeviceId(deviceId);
        } catch (Exception e) {
            log.error("Error finding user by device id: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Получение онлайн пользователей
     */
    public List<User> getOnlineUsers() {
        try {
            LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);
            // Здесь можно добавить логику для получения онлайн пользователей
            // Например, тех, у кого last_seen в последние 5 минут
            return userRepository.findByStatus("ONLINE");
        } catch (Exception e) {
            log.error("Error getting online users: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get online users", e);
        }
    }

    /**
     * Поиск пользователей по имени (частичное совпадение)
     */
    public List<User> searchUsersByName(String name) {
        try {
            // Используем существующий метод и фильтруем на уровне сервиса
            List<User> allUsers = userRepository.findAll();
            return allUsers.stream()
                    .filter(user -> user.getUsername() != null &&
                            user.getUsername().toLowerCase().contains(name.toLowerCase()))
                    .toList();
        } catch (Exception e) {
            log.error("Error searching users by name: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to search users by name", e);
        }
    }
}