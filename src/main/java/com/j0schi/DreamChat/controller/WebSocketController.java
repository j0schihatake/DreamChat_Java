package com.j0schi.DreamChat.controller;

import com.j0schi.DreamChat.enums.MessageStatus;
import com.j0schi.DreamChat.model.Message;
import com.j0schi.DreamChat.model.TypingEvent;
import com.j0schi.DreamChat.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j
public class WebSocketController {

    private final ChatService chatService;

    @MessageMapping("/chat.send")
    @SendTo("/topic/chat/{chatId}")
    public Message sendMessage(Message message, SimpMessageHeaderAccessor headerAccessor) {
        log.info("Received message: {} from user: {}", message.getContent(), message.getSenderId());

        // Устанавливаем статус отправлено
        message.setStatus(MessageStatus.SENT);

        // Отправляем в Kafka
        chatService.sendMessage(message);

        return message;
    }

    @MessageMapping("/chat.typing")
    @SendTo("/topic/typing/{chatId}")
    public TypingEvent handleTyping(TypingEvent typingEvent) {
        log.info("User {} is {} in chat {}",
                typingEvent.getUserId(),
                typingEvent.isTyping() ? "typing" : "stopped typing",
                typingEvent.getChatId());

        return typingEvent;
    }

    @MessageMapping("/chat.seen")
    @SendTo("/topic/seen/{chatId}")
    public MessageStatus handleSeen(String messageId, String userId) {
        log.info("User {} marked message {} as seen", userId, messageId);

        // Обновляем статус сообщения в БД
        chatService.markMessageAsRead(messageId, userId);

        return MessageStatus.READ;
    }

    @MessageMapping("/chat.delete")
    @SendTo("/topic/delete/{chatId}")
    public String handleDelete(String messageId, String userId) {
        log.info("User {} deleting message {}", userId, messageId);

        // Удаляем сообщение (soft delete)
        chatService.deleteMessage(messageId, userId);

        return messageId;
    }
}