package com.j0schi.DreamChat.controller;

import com.j0schi.DreamChat.model.Chat;
import com.j0schi.DreamChat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/chats")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Chat>> getUserChats(@PathVariable String userId) {
        try {
            List<Chat> chats = chatService.findChatsById(userId);
            return ResponseEntity.ok(chats);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{chatId}")
    public ResponseEntity<Chat> getChatById(@PathVariable String chatId) {
        try {
            Chat chat = chatService.getChatById(chatId);
            return chat != null ? ResponseEntity.ok(chat) : ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/private")
    public ResponseEntity<Chat> createPrivateChat(@RequestParam String user1Id,
                                                  @RequestParam String user2Id) {
        try {
            Chat chat = chatService.findOrCreatePrivateChat(user1Id, user2Id);
            return ResponseEntity.ok(chat);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/group")
    public ResponseEntity<Chat> createGroupChat(@RequestBody Chat chat) {
        try {
            Chat createdChat = chatService.createGroupChat(chat);
            return ResponseEntity.ok(createdChat);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{chatId}")
    public ResponseEntity<Chat> updateChat(@PathVariable String chatId, @RequestBody Chat chat) {
        try {
            chat.setId(chatId);
            Chat updatedChat = chatService.updateChat(chat);
            return ResponseEntity.ok(updatedChat);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{chatId}")
    public ResponseEntity<Void> deleteChat(@PathVariable String chatId) {
        try {
            chatService.deleteChat(chatId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{chatId}/participants")
    public ResponseEntity<Void> addParticipant(@PathVariable String chatId,
                                               @RequestParam String userId) {
        try {
            chatService.addParticipantToChat(chatId, userId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{chatId}/participants/{userId}")
    public ResponseEntity<Void> removeParticipant(@PathVariable String chatId,
                                                  @PathVariable String userId) {
        try {
            chatService.removeParticipantFromChat(chatId, userId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}