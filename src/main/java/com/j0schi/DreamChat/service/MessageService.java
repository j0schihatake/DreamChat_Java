package com.j0schi.DreamChat.service;

import com.j0schi.DreamChat.postgres.repository.MessageRepository;
import com.j0schi.DreamChat.postgres.ChatQueryUtil;
import com.j0schi.DreamChat.model.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final ChatQueryUtil chatQueryUtil;

    public void initSchema() {
        messageRepository.execute(chatQueryUtil.createMessageTable());
    }

    public Message saveMessage(Message message) {
        if (messageRepository.save(message)) {
            return messageRepository.findById(message.getId());
        }
        return null;
    }

    public Message getMessageById(String id) {
        return messageRepository.findById(id);
    }

    public List<Message> getChatMessages(String chatId, int limit, int offset) {
        return messageRepository.findByChatId(chatId, limit, offset);
    }

    public List<Message> getChatMessages(String chatId) {
        return messageRepository.findByChatId(chatId);
    }

    /**
     * Новый метод для получения сообщений с пагинацией
     */
    public List<Message> getChatMessagesWithPagination(String chatId, int page, int size) {
        int offset = page * size;
        return messageRepository.findByChatIdWithPagination(chatId, offset, size);
    }

    /**
     * Метод для получения общего количества сообщений в чате
     */
    public long getChatMessagesCount(String chatId) {
        return messageRepository.countByChatId(chatId);
    }

    /**
     * Метод для получения сообщений по диапазону дат
     */
    public List<Message> getChatMessagesByDateRange(String chatId,
                                                    java.time.LocalDateTime startDate,
                                                    java.time.LocalDateTime endDate) {
        return messageRepository.findByChatIdAndTimestampBetween(chatId, startDate, endDate);
    }

    /**
     * Метод для поиска сообщений по содержанию в чате
     */
    public List<Message> searchMessagesInChat(String chatId, String searchText) {
        return messageRepository.findByChatIdAndContentContaining(chatId, searchText);
    }

    public Message getLastChatMessage(String chatId) {
        return messageRepository.findLastMessageByChatId(chatId);
    }

    public boolean updateMessageStatus(String messageId, String status) {
        Message message = messageRepository.findById(messageId);
        if (message != null) {
            message.setStatus(com.j0schi.DreamChat.enums.MessageStatus.valueOf(status));
            return messageRepository.save(message);
        }
        return false;
    }

    /**
     * Пакетное обновление статусов сообщений
     */
    public boolean updateMessageStatusBatch(List<String> messageIds, String status) {
        return messageRepository.updateMessageStatusBatch(messageIds, status);
    }

    /**
     * Удаление сообщения
     */
    public boolean deleteMessage(String messageId) {
        return messageRepository.deleteById(messageId);
    }

    /**
     * Получение непрочитанных сообщений для пользователя в чате
     */
    public long getUnreadMessagesCount(String chatId, String userId) {
        return messageRepository.countUnreadMessages(chatId, userId);
    }

    /**
     * Получение сообщений от определенного отправителя
     */
    public List<Message> getMessagesBySender(String senderId) {
        return messageRepository.findBySenderId(senderId);
    }

    /**
     * Получение сообщений по статусу
     */
    public List<Message> getMessagesByStatus(String status) {
        return messageRepository.findByStatus(status);
    }
}