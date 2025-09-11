package com.j0schi.DreamChat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.j0schi.DreamChat.enums.MessageStatus;
import com.j0schi.DreamChat.model.Chat;
import com.j0schi.DreamChat.model.Message;
import com.j0schi.DreamChat.model.TypingEvent;
import com.j0schi.DreamChat.model.User;
import com.j0schi.DreamChat.repository.ChatRepository;
import com.j0schi.DreamChat.repository.MessageRepository;
import com.j0schi.DreamChat.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;
    private final ChatRepository chatRepository;
    private final MessageRepository messageRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void sendTypingEvent(TypingEvent event) {
        try {
            messagingTemplate.convertAndSend("/topic/typing/" + event.getChatId(), event);
            kafkaTemplate.send("typing-events", event.getUserId(), objectMapper.writeValueAsString(event));
        } catch (Exception e) {
            throw new RuntimeException("Failed to send typing event", e);
        }
    }

    @Transactional
    public void markMessageAsRead(String messageId, String userId) {
        Optional<Message> messageOpt = messageRepository.findById(messageId);
        Optional<User> userOpt = userRepository.findById(userId);

        if (messageOpt.isPresent() && userOpt.isPresent()) {
            Message message = messageOpt.get();
            if (!message.getSender().getId().equals(userId)) {
                message.setStatus(MessageStatus.READ);
                messageRepository.save(message);

                // Отправляем событие
                messagingTemplate.convertAndSend("/topic/read/" + message.getChat().getId(), messageId);
            }
        }
    }

    @Transactional
    public void deleteMessage(String messageId, String userId) {
        Optional<Message> messageOpt = messageRepository.findById(messageId);
        if (messageOpt.isPresent() && messageOpt.get().getSender().getId().equals(userId)) {
            messageRepository.deleteById(messageId);
            messagingTemplate.convertAndSend("/topic/delete/" + messageOpt.get().getChat().getId(), messageId);
        }
    }

    @Transactional
    public Chat findOrCreatePrivateChat(String user1Id, String user2Id) {
        Optional<User> user1Opt = userRepository.findById(user1Id);
        Optional<User> user2Opt = userRepository.findById(user2Id);

        if (user1Opt.isPresent() && user2Opt.isPresent()) {
            User user1 = user1Opt.get();
            User user2 = user2Opt.get();

            // Ищем существующий приватный чат
            Optional<Chat> existingChat = chatRepository.findPrivateChatBetweenUsers(user1, user2);
            if (existingChat.isPresent()) {
                return existingChat.get();
            }

            // Создаем новый чат
            Chat newChat = new Chat();
            newChat.setType(com.j0schi.DreamChat.enums.ChatType.PRIVATE);
            newChat.setTitle(user2.getUsername());
            newChat.getParticipants().add(user1);
            newChat.getParticipants().add(user2);

            return chatRepository.save(newChat);
        }

        throw new RuntimeException("Users not found");
    }

    @Transactional
    public Message sendMessage(Message message) {
        try {
            // Загружаем полные объекты из БД
            Optional<User> senderOpt = userRepository.findById(message.getSender().getId());
            Optional<Chat> chatOpt = chatRepository.findById(message.getChat().getId());

            if (senderOpt.isEmpty() || chatOpt.isEmpty()) {
                throw new RuntimeException("Sender or chat not found");
            }

            message.setSender(senderOpt.get());
            message.setChat(chatOpt.get());

            // Сохраняем сообщение в БД
            Message savedMessage = messageRepository.save(message);

            // Обновляем последнее сообщение в чате
            Chat chat = chatOpt.get();
            chat.setLastMessage(savedMessage);
            chatRepository.save(chat);

            // Отправляем в Kafka
            String topic = "chat-messages-" + message.getChat().getId();
            kafkaTemplate.send(topic, message.getId(), objectMapper.writeValueAsString(message));

            // Отправляем через WebSocket
            messagingTemplate.convertAndSend("/topic/chat/" + message.getChat().getId(), message);

            return savedMessage;
        } catch (Exception e) {
            throw new RuntimeException("Failed to send message", e);
        }
    }

    public List<Message> getChatHistory(String chatId, int page, int size) {
        return messageRepository.findByChatIdOrderByTimestampDesc(
                chatId, PageRequest.of(page, size));
    }
}