package com.j0schi.DreamChat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.j0schi.DreamChat.enums.ChatType;
import com.j0schi.DreamChat.enums.MessageStatus;
import com.j0schi.DreamChat.model.Chat;
import com.j0schi.DreamChat.model.Message;
import com.j0schi.DreamChat.model.TypingEvent;
import com.j0schi.DreamChat.model.User;
import com.j0schi.DreamChat.repository.ChatRepository;
import com.j0schi.DreamChat.repository.MessageRepository;
import com.j0schi.DreamChat.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;
    private final ChatRepository chatRepository;
    private final MessageRepository messageRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<Chat> getUserChats(String userId) {
        log.info("Getting chats for user: {}", userId);

        try {
            // Сначала убедимся, что у пользователя есть чат с самим собой
            Chat selfChat = findOrCreateSelfChat(userId);
            log.info("Self chat: {} - {}", selfChat.getId(), selfChat.getTitle());

            // Затем получаем все чаты пользователя
            List<Chat> userChats = chatRepository.findByUserId(userId);
            log.info("Found {} chats from database", userChats.size());

            // Проверяем, есть ли уже чат с самим собой в списке
            boolean hasSelfChat = userChats.stream()
                    .anyMatch(chat -> chat.getId().equals(selfChat.getId()));

            log.info("Self chat already in list: {}", hasSelfChat);

            // Если нет, добавляем его
            if (!hasSelfChat) {
                userChats.add(0, selfChat);
                log.info("Added self chat to list");
            }

            log.info("Total chats returned: {}", userChats.size());
            return userChats;

        } catch (Exception e) {
            log.error("Error getting chats for user {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to get user chats", e);
        }
    }

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
        log.info("Finding or creating private chat between {} and {}", user1Id, user2Id);

        // Если это запрос на чат с самим собой
        if (user1Id.equals(user2Id)) {
            return findOrCreateSelfChat(user1Id);
        }

        Optional<User> user1Opt = userRepository.findById(user1Id);
        Optional<User> user2Opt = userRepository.findById(user2Id);

        if (user1Opt.isEmpty() || user2Opt.isEmpty()) {
            throw new RuntimeException("Users not found");
        }

        User user1 = user1Opt.get();
        User user2 = user2Opt.get();

        // Пробуем найти существующий приватный чат
        Optional<Chat> existingChat = chatRepository.findPrivateChatByUserIds(user1Id, user2Id);
        if (existingChat.isPresent()) {
            log.info("Found existing chat: {}", existingChat.get().getId());
            return existingChat.get();
        }

        // Создаем новый чат
        Chat newChat = new Chat();
        newChat.setType(ChatType.PRIVATE);
        newChat.setTitle(user2.getUsername() + " & " + user1.getUsername());
        newChat.getParticipants().add(user1);
        newChat.getParticipants().add(user2);

        Chat savedChat = chatRepository.save(newChat);
        log.info("Created new chat: {}", savedChat.getId());
        return savedChat;
    }

    @Transactional
    public Chat findOrCreateSelfChat(String userId) {
        log.info("Finding or creating self chat for user: {}", userId);

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("User not found");
        }

        User user = userOpt.get();

        // Ищем чат "Saved Messages" для этого пользователя
        Optional<Chat> existingSelfChat = chatRepository.findByTitleAndUser("Saved Messages", userId);

        if (existingSelfChat.isPresent()) {
            log.info("Found existing self chat: {}", existingSelfChat.get().getId());
            return existingSelfChat.get();
        }

        // Создаем новый чат с самим собой
        Chat selfChat = new Chat();
        selfChat.setType(ChatType.PRIVATE);
        selfChat.setTitle("Saved Messages");
        selfChat.getParticipants().add(user);

        Chat savedChat = chatRepository.save(selfChat);
        log.info("Created new self chat: {}", savedChat.getId());
        return savedChat;
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