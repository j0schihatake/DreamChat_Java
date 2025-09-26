package com.j0schi.DreamChat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.j0schi.DreamChat.enums.ChatType;
import com.j0schi.DreamChat.enums.MessageStatus;
import com.j0schi.DreamChat.model.Chat;
import com.j0schi.DreamChat.model.Message;
import com.j0schi.DreamChat.model.TypingEvent;
import com.j0schi.DreamChat.model.User;
import com.j0schi.DreamChat.postgres.repository.ChatRepository;
import com.j0schi.DreamChat.postgres.repository.MessageRepository;
import com.j0schi.DreamChat.postgres.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;
    private final ChatRepository chatRepository;
    private final MessageRepository messageRepository;
    private final ObjectMapper objectMapper;

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

    /**
     * Метод возвращает чат по chatId
     */
    public Chat getChatById(String chatId) {
        try {
            Chat chat = chatRepository.findById(chatId);
            if (chat == null) {
                log.warn("Chat not found with id: {}", chatId);
                throw new RuntimeException("Chat not found");
            }

            // Загружаем участников чата
            List<User> participants = chatRepository.getChatParticipants(chatId);
            chat.getParticipants().addAll(participants);

            return chat;
        } catch (Exception e) {
            log.error("ChatById not found: {}", e.getMessage());
            throw new RuntimeException("Failed to get chat by id", e);
        }
    }

    /**
     * Создание группового чата
     */
    @Transactional
    public Chat createGroupChat(Chat chat) {
        try {
            log.info("Creating group chat: {}", chat.getTitle());

            // Генерируем ID если не установлен
            if (chat.getId() == null) {
                chat.setId(UUID.randomUUID().toString());
            }

            // Устанавливаем тип и дату создания
            chat.setType(ChatType.GROUP);
            chat.setCreatedAt(LocalDateTime.now());

            // Сохраняем чат
            boolean saved = chatRepository.save(chat);
            if (!saved) {
                throw new RuntimeException("Failed to save group chat");
            }

            // Добавляем участников
            for (User participant : chat.getParticipants()) {
                chatRepository.addParticipantToChat(chat.getId(), participant.getId());
            }

            log.info("Group chat created successfully: {}", chat.getId());
            return chat;

        } catch (Exception e) {
            log.error("Error creating group chat: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create group chat", e);
        }
    }

    /**
     * Обновление чата
     */
    @Transactional
    public Chat updateChat(Chat chat) {
        try {
            log.info("Updating chat: {}", chat.getId());

            // Проверяем существование чата
            Chat existingChat = chatRepository.findById(chat.getId());
            if (existingChat == null) {
                throw new RuntimeException("Chat not found");
            }

            // Обновляем поля
            if (chat.getTitle() != null) {
                existingChat.setTitle(chat.getTitle());
            }
            if (chat.getAvatarUrl() != null) {
                existingChat.setAvatarUrl(chat.getAvatarUrl());
            }
            if (chat.getType() != null) {
                existingChat.setType(chat.getType());
            }
            if (chat.getUnreadCount() >= 0) {
                existingChat.setUnreadCount(chat.getUnreadCount());
            }

            // Сохраняем изменения
            boolean updated = chatRepository.save(existingChat);
            if (!updated) {
                throw new RuntimeException("Failed to update chat");
            }

            log.info("Chat updated successfully: {}", chat.getId());
            return existingChat;

        } catch (Exception e) {
            log.error("Error updating chat: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update chat", e);
        }
    }

    /**
     * Удаление чата
     */
    @Transactional
    public void deleteChat(String chatId) {
        try {
            log.info("Deleting chat: {}", chatId);

            boolean deleted = chatRepository.deleteById(chatId);
            if (!deleted) {
                throw new RuntimeException("Failed to delete chat");
            }

            // Также удаляем все сообщения этого чата
            List<Message> chatMessages = messageRepository.findByChatId(chatId);
            for (Message message : chatMessages) {
                messageRepository.deleteById(message.getId());
            }

            log.info("Chat deleted successfully: {}", chatId);

        } catch (Exception e) {
            log.error("Error deleting chat: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to delete chat", e);
        }
    }

    /**
     * Добавление участника в чат
     */
    @Transactional
    public void addParticipantToChat(String chatId, String userId) {
        try {
            log.info("Adding user {} to chat {}", userId, chatId);

            // Проверяем существование чата и пользователя
            Chat chat = chatRepository.findById(chatId);
            User user = userRepository.findById(userId);

            if (chat == null) {
                throw new RuntimeException("Chat not found");
            }
            if (user == null) {
                throw new RuntimeException("User not found");
            }

            // Проверяем, не является ли пользователь уже участником
            List<User> participants = chatRepository.getChatParticipants(chatId);
            boolean alreadyParticipant = participants.stream()
                    .anyMatch(p -> p.getId().equals(userId));

            if (alreadyParticipant) {
                log.info("User {} is already participant of chat {}", userId, chatId);
                return;
            }

            // Добавляем участника
            boolean added = chatRepository.addParticipantToChat(chatId, userId);
            if (!added) {
                throw new RuntimeException("Failed to add participant to chat");
            }

            log.info("User {} added to chat {} successfully", userId, chatId);

        } catch (Exception e) {
            log.error("Error adding participant to chat: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to add participant to chat", e);
        }
    }

    /**
     * Удаление участника из чата
     */
    @Transactional
    public void removeParticipantFromChat(String chatId, String userId) {
        try {
            log.info("Removing user {} from chat {}", userId, chatId);

            // Проверяем существование чата
            Chat chat = chatRepository.findById(chatId);
            if (chat == null) {
                throw new RuntimeException("Chat not found");
            }

            // Удаляем участника
            boolean removed = chatRepository.removeParticipantFromChat(chatId, userId);
            if (!removed) {
                throw new RuntimeException("Failed to remove participant from chat");
            }

            log.info("User {} removed from chat {} successfully", userId, chatId);

        } catch (Exception e) {
            log.error("Error removing participant from chat: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to remove participant from chat", e);
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
        try {
            Optional<Message> messageOpt = Optional.ofNullable(messageRepository.findById(messageId));
            Optional<User> userOpt = Optional.ofNullable(userRepository.findById(userId));

            if (messageOpt.isPresent() && userOpt.isPresent()) {
                Message message = messageOpt.get();
                if (!message.getSender().getId().equals(userId)) {
                    message.setStatus(MessageStatus.READ);
                    boolean saved = messageRepository.save(message);

                    if (saved) {
                        // Отправляем событие
                        messagingTemplate.convertAndSend("/topic/read/" + message.getChat().getId(), messageId);
                        log.info("Message {} marked as read by user {}", messageId, userId);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error marking message as read: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to mark message as read", e);
        }
    }

    @Transactional
    public void deleteMessage(String messageId, String userId) {
        try {
            Optional<Message> messageOpt = Optional.ofNullable(messageRepository.findById(messageId));
            if (messageOpt.isPresent() && messageOpt.get().getSender().getId().equals(userId)) {
                boolean deleted = messageRepository.deleteById(messageId);
                if (deleted) {
                    messagingTemplate.convertAndSend("/topic/delete/" + messageOpt.get().getChat().getId(), messageId);
                    log.info("Message {} deleted by user {}", messageId, userId);
                }
            }
        } catch (Exception e) {
            log.error("Error deleting message: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to delete message", e);
        }
    }

    @Transactional
    public List<Chat> findChatsById(String userId) {
        try {
            return chatRepository.findByUserId(userId);
        } catch (Exception e) {
            log.error("Error finding chats by user id: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to find chats by user id", e);
        }
    }

    @Transactional
    public Chat findOrCreatePrivateChat(String user1Id, String user2Id) {
        log.info("Finding or creating private chat between {} and {}", user1Id, user2Id);

        // Если это запрос на чат с самим собой
        if (user1Id.equals(user2Id)) {
            return findOrCreateSelfChat(user1Id);
        }

        Optional<User> user1Opt = Optional.ofNullable(userRepository.findById(user1Id));
        Optional<User> user2Opt = Optional.ofNullable(userRepository.findById(user2Id));

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
        newChat.setId(UUID.randomUUID().toString());
        newChat.setType(ChatType.PRIVATE);
        newChat.setTitle(user2.getUsername() + " & " + user1.getUsername());
        newChat.setCreatedAt(LocalDateTime.now());

        // Сохраняем чат
        boolean saved = chatRepository.save(newChat);
        if (!saved) {
            throw new RuntimeException("Failed to save private chat");
        }

        // Добавляем участников
        chatRepository.addParticipantToChat(newChat.getId(), user1Id);
        chatRepository.addParticipantToChat(newChat.getId(), user2Id);

        // Добавляем участников в объект
        newChat.getParticipants().add(user1);
        newChat.getParticipants().add(user2);

        log.info("Private chat created: {}", newChat.getId());
        return newChat;
    }

    @Transactional
    public Chat findOrCreateSelfChat(String userId) {
        log.info("Finding or creating self chat for user: {}", userId);

        Optional<User> userOpt = Optional.ofNullable(userRepository.findById(userId));
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
        selfChat.setId(UUID.randomUUID().toString());
        selfChat.setType(ChatType.PRIVATE);
        selfChat.setTitle("Saved Messages");
        selfChat.setCreatedAt(LocalDateTime.now());

        // Сохраняем чат
        boolean saved = chatRepository.save(selfChat);
        if (!saved) {
            throw new RuntimeException("Failed to save self chat");
        }

        // Добавляем участника (самого себя)
        chatRepository.addParticipantToChat(selfChat.getId(), userId);
        selfChat.getParticipants().add(user);

        log.info("Created new self chat: {}", selfChat.getId());
        return selfChat;
    }

    @Transactional
    public Message sendMessage(Message message) {
        try {
            // Генерируем ID если не установлен
            if (message.getId() == null) {
                message.setId(UUID.randomUUID().toString());
            }

            // Устанавливаем timestamp если не установлен
            if (message.getTimestamp() == null) {
                message.setTimestamp(LocalDateTime.now());
            }

            // Устанавливаем статус если не установлен
            if (message.getStatus() == null) {
                message.setStatus(MessageStatus.SENT);
            }

            // Сохраняем сообщение в БД
            boolean saved = saveMessage(message);
            if (!saved) {
                throw new RuntimeException("Failed to save message");
            }

            // Обновляем последнее сообщение в чате
            Chat chat = chatRepository.findById(message.getChatId());
            if (chat != null) {
                chat.setLastMessage(message);
                chatRepository.updateLastMessage(chat.getId(), message.getId());
            }

            // Отправляем в Kafka
            String topic = "chat-messages-" + message.getChatId();
            kafkaTemplate.send(topic, message.getId(), objectMapper.writeValueAsString(message));

            // Отправляем через WebSocket
            messagingTemplate.convertAndSend("/topic/chat/" + message.getChatId(), message);

            log.info("Message sent successfully: {}", message.getId());
            return message;
        } catch (Exception e) {
            log.error("Failed to send message: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send message", e);
        }
    }

    public boolean saveMessage2(Message message) {
        if (message.getId() != null && messageRepository.findById(message.getId()) != null){
            return messageRepository.save(message);
        } else {
            return messageRepository.save(message);
        }
    }

    public boolean saveMessage(Message message) {
        try {
            return messageRepository.save(message);
        } catch (Exception e) {
            log.error("Error saving message: {}", e.getMessage(), e);
            return false;
        }
    }

    public List<Message> getChatHistory(String chatId, int page, int size) {
        try {
            // Используем новый метод пагинации
            return messageRepository.findByChatIdWithPagination(chatId, page * size, size);
        } catch (Exception e) {
            log.error("Error getting chat history: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get chat history", e);
        }
    }
}