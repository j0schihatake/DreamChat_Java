package com.j0schi.DreamChat.controller;

import com.j0schi.DreamChat.model.Chat;
import com.j0schi.DreamChat.model.User;
import com.j0schi.DreamChat.postgres.repository.UserRepository;
import com.j0schi.DreamChat.service.ChatService;
import com.j0schi.DreamChat.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final ChatService chatService;
    private final UserService userService;

    @GetMapping("/{userId}")
    public ResponseEntity<User> getUserById(@PathVariable String userId) {
        try {
            User user = userRepository.findById(userId);
            return user != null ? ResponseEntity.ok(user) : ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchUser(@RequestParam(required = false) String phoneNumber,
                                        @RequestParam(required = false) String username,
                                        @RequestParam(required = false) String currentUserId) {
        try {
            if (phoneNumber != null) {
                String normalizedPhone = phoneNumber.replaceAll("[^0-9]", "");

                // Если пользователь ищет самого себя
                if (currentUserId != null) {
                    Optional<User> currentUser = Optional.ofNullable(userRepository.findById(currentUserId));
                    if (currentUser.isPresent() &&
                            currentUser.get().getPhoneNumber().equals(normalizedPhone)) {
                        Chat selfChat = chatService.findOrCreateSelfChat(currentUserId);

                        // Возвращаем универсальный ответ
                        Map<String, Object> response = new HashMap<>();
                        response.put("type", "chat");
                        response.put("data", selfChat);
                        return ResponseEntity.ok(response);
                    }
                }

                Optional<User> user = userRepository.findByPhoneNumber(normalizedPhone);
                if (user.isPresent()) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("type", "user");
                    response.put("data", user.get());
                    return ResponseEntity.ok(response);
                } else {
                    return ResponseEntity.notFound().build();
                }
            } else if (username != null) {
                Optional<User> user = userRepository.findByUsername(username);
                if (user.isPresent()) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("type", "user");
                    response.put("data", user.get());
                    return ResponseEntity.ok(response);
                } else {
                    return ResponseEntity.notFound().build();
                }
            } else {
                return ResponseEntity.badRequest().body("Must provide phoneNumber or username");
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/contacts")
    public ResponseEntity<String> addContact(@RequestBody User user) {
        // В реальном приложении здесь была бы логика добавления в контакты
        return ResponseEntity.ok("Contact added");
    }

    @GetMapping("/{userId}/chats")
    public ResponseEntity<List<Chat>> getUserChats(@PathVariable String userId) {
        try {
            List<Chat> chats = chatService.findChatsById(userId);
            return ResponseEntity.ok(chats);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{userId}/chats")
    public ResponseEntity<Chat> createChatWithUser(@PathVariable String userId,
                                                   @RequestParam String targetUserId) {
        try {
            Chat chat = chatService.findOrCreatePrivateChat(userId, targetUserId);
            return ResponseEntity.ok(chat);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{userId}")
    public ResponseEntity<User> updateUser(@PathVariable String userId, @RequestBody User user) {
        try {
            User updatedUser = userService.updateUser(userId, user);
            return ResponseEntity.ok(updatedUser);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // Добавляем новые endpoint'ы
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        try {
            List<User> users = userService.getAllUsers();
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/search/name")
    public ResponseEntity<List<User>> searchUsersByName(@RequestParam String name) {
        try {
            List<User> users = userService.searchUsersByName(name);
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{userId}/status")
    public ResponseEntity<Void> updateUserStatus(@PathVariable String userId,
                                                 @RequestParam String status) {
        try {
            boolean updated = userService.updateUserStatus(userId, status);
            return updated ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{userId}/last-seen")
    public ResponseEntity<Void> updateLastSeen(@PathVariable String userId) {
        try {
            boolean updated = userService.updateLastSeen(userId, LocalDateTime.now());
            return updated ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}