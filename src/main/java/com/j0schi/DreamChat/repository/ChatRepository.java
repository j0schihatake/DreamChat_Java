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

    @Query("SELECT c FROM Chat c WHERE c.type = 'PRIVATE' AND " +
            "EXISTS (SELECT 1 FROM c.participants p WHERE p.id = :user1Id) AND " +
            "EXISTS (SELECT 1 FROM c.participants p WHERE p.id = :user2Id)")
    Optional<Chat> findPrivateChatBetweenUsers(@Param("user1Id") String user1Id,
                                               @Param("user2Id") String user2Id);

    @Query("SELECT c FROM Chat c WHERE c.title = :title AND " +
            "EXISTS (SELECT 1 FROM c.participants p WHERE p.id = :userId)")
    Optional<Chat> findByTitleAndUser(@Param("title") String title,
                                      @Param("userId") String userId);

    @Query("SELECT DISTINCT c FROM Chat c JOIN c.participants p WHERE p.id = :userId ORDER BY c.createdAt DESC")
    List<Chat> findByUserId(@Param("userId") String userId);

    @Query("SELECT c FROM Chat c JOIN c.participants p WHERE p.id = :userId " +
            "ORDER BY COALESCE(c.lastMessage.timestamp, c.createdAt) DESC")
    List<Chat> findByUserIdOrderByLastMessageTimestampDesc(@Param("userId") String userId);

    // Удаляем нереализованный метод и добавляем правильную реализацию
    @Query("SELECT c FROM Chat c WHERE c.type = 'PRIVATE' AND " +
            "EXISTS (SELECT 1 FROM c.participants p WHERE p.id = :user1Id) AND " +
            "EXISTS (SELECT 1 FROM c.participants p WHERE p.id = :user2Id) AND " +
            "SIZE(c.participants) = 2")
    Optional<Chat> findPrivateChatByUserIds(@Param("user1Id") String user1Id,
                                            @Param("user2Id") String user2Id);
}