package com.j0schi.DreamChat.model;

import com.j0schi.DreamChat.enums.ChatType;
import lombok.Data;

import java.util.Set;

@Data
public class Chat {
    private String id;
    private ChatType type;
    private Set<String> participantIds;
    private String name;
    private String avatarUrl;
}