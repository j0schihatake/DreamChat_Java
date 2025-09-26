package com.j0schi.DreamChat.controller;

import com.j0schi.DreamChat.dto.MessageStatusUpdateDTO;
import com.j0schi.DreamChat.service.MessageService;
import com.j0schi.DreamChat.model.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class MessageController {

    private final SimpMessagingTemplate messagingTemplate;
    private final MessageService messageService;

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
