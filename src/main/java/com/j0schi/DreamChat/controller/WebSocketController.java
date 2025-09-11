package com.j0schi.DreamChat.controller;

import com.j0schi.DreamChat.dto.MessageDTO;
import com.j0schi.DreamChat.mapper.MessageMapper;
import com.j0schi.DreamChat.model.Message;
import com.j0schi.DreamChat.model.TypingEvent;
import com.j0schi.DreamChat.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j
public class WebSocketController {

    private final ChatService chatService;
    private final MessageMapper messageMapper;

    @MessageMapping("/chat.send.{chatId}")
    @SendTo("/topic/chat/{chatId}")
    public MessageDTO sendMessage(@DestinationVariable String chatId,
                                  MessageDTO messageDTO,
                                  SimpMessageHeaderAccessor headerAccessor) {
        log.info("Received message for chat {}: {}", chatId, messageDTO.getContent());

        try {
            Message message = messageMapper.toEntity(messageDTO);
            message.setChatId(chatId);

            Message sentMessage = chatService.sendMessage(message);
            return messageMapper.toDTO(sentMessage);
        } catch (Exception e) {
            log.error("Failed to process message", e);
            // Устанавливаем статус ошибки
            messageDTO.setStatus(com.j0schi.DreamChat.enums.MessageStatus.FAILED);
            return messageDTO;
        }
    }

    @MessageMapping("/chat.typing.{chatId}")
    @SendTo("/topic/typing/{chatId}")
    public TypingEvent handleTyping(@DestinationVariable String chatId,
                                    TypingEvent typingEvent) {
        log.info("User {} is {} in chat {}",
                typingEvent.getUserId(),
                typingEvent.isTyping() ? "typing" : "stopped typing",
                chatId);

        typingEvent.setChatId(chatId);
        chatService.sendTypingEvent(typingEvent);

        return typingEvent;
    }

    @MessageMapping("/chat.seen.{chatId}")
    @SendTo("/topic/seen/{chatId}")
    public String handleSeen(@DestinationVariable String chatId,
                             String messageId,
                             String userId) {
        log.info("User {} marked message {} as seen in chat {}", userId, messageId, chatId);

        chatService.markMessageAsRead(messageId, userId);
        return messageId;
    }

    @MessageMapping("/chat.delete.{chatId}")
    @SendTo("/topic/delete/{chatId}")
    public String handleDelete(@DestinationVariable String chatId,
                               String messageId,
                               String userId) {
        log.info("User {} deleting message {} in chat {}", userId, messageId, chatId);

        chatService.deleteMessage(messageId, userId);
        return messageId;
    }
}