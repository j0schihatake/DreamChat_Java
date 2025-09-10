package com.j0schi.DreamChat.model;

import com.j0schi.DreamChat.enums.UserStatus;
import lombok.Data;

@Data
public class User {
    private String id;
    private String username;
    private String email;
    private String avatarUrl;
    private UserStatus status; // ONLINE, OFFLINE, AWAY
}