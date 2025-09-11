package com.j0schi.DreamChat.controller;

import com.j0schi.DreamChat.dto.MessageDTO;
import com.j0schi.DreamChat.mapper.MessageMapper;
import com.j0schi.DreamChat.model.Message;
import com.j0schi.DreamChat.model.TypingEvent;
import com.j0schi.DreamChat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatRestController {

    private final ChatService chatService;
    private final MessageMapper messageMapper;

    @PostMapping("/message")
    public ResponseEntity<MessageDTO> sendMessage(@RequestBody MessageDTO messageDTO) {
        try {
            Message message = messageMapper.toEntity(messageDTO);
            Message sentMessage = chatService.sendMessage(message);
            return ResponseEntity.ok(messageMapper.toDTO(sentMessage));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
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
    public ResponseEntity<List<MessageDTO>> getChatHistory(@PathVariable String chatId,
                                                           @RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "50") int size) {
        List<Message> messages = chatService.getChatHistory(chatId, page, size);
        List<MessageDTO> dtos = messages.stream()
                .map(messageMapper::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }
}