package com.j0schi.DreamChat.repository;

import com.j0schi.DreamChat.model.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, String> {

    List<Message> findByChatIdOrderByTimestampDesc(String chatId, Pageable pageable);

    @Query("SELECT m FROM Message m WHERE m.chat.id = :chatId ORDER BY m.timestamp DESC")
    List<Message> findLatestMessages(@Param("chatId") String chatId, Pageable pageable);

    // Упрощенный метод для подсчета непрочитанных сообщений
    @Query("SELECT COUNT(m) FROM Message m WHERE m.chat.id = :chatId AND m.status != 'READ' AND m.sender.id != :senderId")
    long countUnreadMessages(@Param("chatId") String chatId, @Param("senderId") String senderId);
}