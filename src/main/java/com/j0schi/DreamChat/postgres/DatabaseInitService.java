package com.j0schi.DreamChat.postgres;

import com.j0schi.DreamChat.postgres.repository.ChatRepository;
import com.j0schi.DreamChat.postgres.repository.MessageRepository;
import com.j0schi.DreamChat.postgres.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DatabaseInitService {

    private final ChatQueryUtil chatQueryUtil;
    private final UserRepository userRepository;
    private final ChatRepository chatRepository;
    private final MessageRepository messageRepository;

    @PostConstruct
    public void initSchema() {
        // Создаем таблицы в правильном порядке из-за foreign keys
        userRepository.execute(chatQueryUtil.createUserTable());
        chatRepository.execute(chatQueryUtil.createChatTable());
        chatRepository.execute(chatQueryUtil.createChatParticipantsTable());
        messageRepository.execute(chatQueryUtil.createMessageTable());

        System.out.println("Database schema initialized successfully");
    }
}