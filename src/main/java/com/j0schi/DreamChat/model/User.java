package com.j0schi.DreamChat.model;

import com.j0schi.DreamChat.enums.UserStatus;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(unique = true, nullable = false)
    private String phoneNumber;

    @Column(unique = true)
    private String username;

    private String email;
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    private UserStatus status = UserStatus.OFFLINE;

    private LocalDateTime lastSeen;
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private String deviceId;

    private boolean isAuthorized = false;
}