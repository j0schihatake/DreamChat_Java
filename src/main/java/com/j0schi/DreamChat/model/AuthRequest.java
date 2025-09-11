package com.j0schi.DreamChat.model;

import lombok.Data;

@Data
public class AuthRequest {
    private String phoneNumber;
    private String deviceId;
}