package com.j0schi.DreamChat.controller;

import com.j0schi.DreamChat.dto.MessageStatusUpdateDTO;
import com.j0schi.DreamChat.service.MessageService;
import com.j0schi.DreamChat.model.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final SimpMessagingTemplate messagingTemplate;
    private final MessageService messageService;

    @GetMapping("/chat/{chatId}")
    public ResponseEntity<List<Message>> getChatMessages(
            @PathVariable String chatId,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        try {
            List<Message> messages = messageService.getChatMessages(chatId, limit, offset);
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{messageId}")
    public ResponseEntity<Message> getMessageById(@PathVariable String messageId) {
        try {
            Message message = messageService.getMessageById(messageId);
            return message != null ? ResponseEntity.ok(message) : ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{messageId}")
    public ResponseEntity<Void> deleteMessage(@PathVariable String messageId) {
        try {
            messageService.deleteMessage(messageId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload Message message) {
        // Сохраняем сообщение в БД
        Message savedMessage = messageService.saveMessage(message);

        if (savedMessage != null) {
            // Отправляем сообщение всем подписчикам чата
            messagingTemplate.convertAndSend(
                    "/topic/chat/" + savedMessage.getChatId(),
                    savedMessage
            );
        }
    }

    @MessageMapping("/chat.updateStatus")
    public void updateMessageStatus(@Payload MessageStatusUpdateDTO update) {
        boolean updated = messageService.updateMessageStatus(
                update.getMessageId(), update.getStatus()
        );

        if (updated) {
            Message message = messageService.getMessageById(update.getMessageId());
            messagingTemplate.convertAndSend(
                    "/topic/chat/" + message.getChatId() + "/status",
                    message
            );
        }
    }
}
