package com.j0schi.DreamChat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.j0schi.DreamChat.enums.MessageStatus;
import com.j0schi.DreamChat.model.Message;
import com.j0schi.DreamChat.model.TypingEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public void sendMessage(Message message) {
        String topic = "chat-messages-" + message.getChatId();
        String key = message.getId();
        String jsonMessage = convertToJson(message);

        kafkaTemplate.send(topic, key, jsonMessage)
                .thenAccept(result -> {
                    log.info("Message sent to Kafka: topic={}, partition={}, offset={}",
                            result.getRecordMetadata().topic(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                })
                .exceptionally(ex -> {
                    log.error("Failed to send message to Kafka", ex);
                    return null;
                });
    }

    public void sendTypingEvent(TypingEvent event) {
        String topic = "typing-events-" + event.getChatId();
        String key = event.getUserId();
        String jsonEvent = convertToJson(event);

        kafkaTemplate.send(topic, key, jsonEvent);
    }

    public CompletableFuture<Void> sendMessageToUser(String userId, Message message) {
        String topic = "user-messages-" + userId;
        return kafkaTemplate.send(topic, message.getId(), convertToJson(message))
                .thenAccept(result -> {
                    updateMessageStatus(message.getId(), MessageStatus.DELIVERED);
                });
    }

    public void markMessageAsRead(String messageId, String userId) {
        // Логика обновления статуса сообщения в БД
        log.info("Marking message {} as read by user {}", messageId, userId);

        // Можно также отправить событие в Kafka
        String topic = "message-read-events";
        String eventData = "{\"messageId\":\"" + messageId + "\",\"userId\":\"" + userId + "\"}";
        kafkaTemplate.send(topic, messageId, eventData);
    }

    public void deleteMessage(String messageId, String userId) {
        // Логика soft delete сообщения
        log.info("Soft deleting message {} by user {}", messageId, userId);

        // Событие удаления в Kafka
        String topic = "message-delete-events";
        String eventData = "{\"messageId\":\"" + messageId + "\",\"userId\":\"" + userId + "\"}";
        kafkaTemplate.send(topic, messageId, eventData);
    }

    private void updateMessageStatus(String messageId, MessageStatus status) {
        // Логика обновления статуса в БД
        log.info("Updating message {} status to {}", messageId, status);
    }

    private String convertToJson(Object object) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            return mapper.writeValueAsString(object);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert to JSON", e);
        }
    }
}