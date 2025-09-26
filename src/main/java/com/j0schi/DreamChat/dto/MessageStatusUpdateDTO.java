package com.j0schi.DreamChat.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MessageStatusUpdateDTO {
    private String messageId;
    private String status;
}