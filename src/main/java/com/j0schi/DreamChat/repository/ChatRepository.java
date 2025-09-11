package com.j0schi.DreamChat.repository;

import com.j0schi.DreamChat.model.Chat;
import com.j0schi.DreamChat.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRepository extends JpaRepository<Chat, String> {
    List<Chat> findByParticipantsContaining(User user);

    @Query("SELECT c FROM Chat c WHERE c.type = 'PRIVATE' AND :user1 MEMBER OF c.participants AND :user2 MEMBER OF c.participants")
    Optional<Chat> findPrivateChatBetweenUsers(@Param("user1") User user1, @Param("user2") User user2);

    @Query("SELECT c FROM Chat c JOIN c.participants p WHERE p.id = :userId ORDER BY c.lastMessage.timestamp DESC")
    List<Chat> findByUserIdOrderByLastMessageTimestampDesc(@Param("userId") String userId);
}