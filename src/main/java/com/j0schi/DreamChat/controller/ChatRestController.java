package com.j0schi.DreamChat.controller;

import com.j0schi.DreamChat.model.Message;
import com.j0schi.DreamChat.model.TypingEvent;
import com.j0schi.DreamChat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatRestController {

    private final ChatService chatService;

    @PostMapping("/message")
    public CompletableFuture<ResponseEntity<Message>> sendMessage(@RequestBody Message message) {
        return CompletableFuture.supplyAsync(() -> {
            chatService.sendMessage(message);
            return ResponseEntity.ok(message);
        });
    }

    @PostMapping("/typing")
    public ResponseEntity<TypingEvent> sendTypingEvent(@RequestBody TypingEvent event) {
        chatService.sendTypingEvent(event);
        return ResponseEntity.ok(event);
    }

    @PostMapping("/{messageId}/read")
    public ResponseEntity<String> markAsRead(@PathVariable String messageId,
                                             @RequestParam String userId) {
        chatService.markMessageAsRead(messageId, userId);
        return ResponseEntity.ok("Message marked as read");
    }

    @DeleteMapping("/{messageId}")
    public ResponseEntity<String> deleteMessage(@PathVariable String messageId,
                                                @RequestParam String userId) {
        chatService.deleteMessage(messageId, userId);
        return ResponseEntity.ok("Message deleted");
    }

    @GetMapping("/{chatId}/messages")
    public ResponseEntity<List<Message>> getChatHistory(@PathVariable String chatId,
                                                        @RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "50") int size) {
        // Здесь будет логика получения истории сообщений из БД
        return ResponseEntity.ok(List.of());
    }
}