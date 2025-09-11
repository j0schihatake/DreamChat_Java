package com.j0schi.DreamChat.controller;

import com.j0schi.DreamChat.model.Chat;
import com.j0schi.DreamChat.model.User;
import com.j0schi.DreamChat.service.ChatService;
import com.j0schi.DreamChat.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final ChatService chatService;

    @GetMapping("/search")
    public ResponseEntity<User> searchUser(@RequestParam String phoneNumber) {
        Optional<User> user = userRepository.findByPhoneNumber(phoneNumber.replaceAll("[^0-9]", ""));
        return user.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/contacts")
    public ResponseEntity<String> addContact(@RequestBody User user) {
        // В реальном приложении здесь была бы логика добавления в контакты
        return ResponseEntity.ok("Contact added");
    }

    @GetMapping("/{userId}/chats")
    public ResponseEntity<List<Chat>> getUserChats(@PathVariable String userId) {
        // Здесь будет логика получения чатов пользователя
        return ResponseEntity.ok(List.of());
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
}