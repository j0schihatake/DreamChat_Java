package com.j0schi.DreamChat.model;

import com.j0schi.DreamChat.enums.UserStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class User {

    private String id;
    private String phoneNumber;
    private String username;
    private String email;
    private String avatarUrl;
    private String deviceId;
    private boolean isAuthorized = false;

    private LocalDateTime lastSeen;
    private LocalDateTime createdAt = LocalDateTime.now();

    private UserStatus status = UserStatus.OFFLINE;
}