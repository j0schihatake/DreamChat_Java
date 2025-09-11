package com.j0schi.DreamChat.model;

import lombok.Data;

@Data
public class AuthResponse {
    private boolean success;
    private String userId;
    private String username;
    private String message;

    public AuthResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public AuthResponse(boolean success, String userId, String username, String message) {
        this.success = success;
        this.userId = userId;
        this.username = username;
        this.message = message;
    }
}