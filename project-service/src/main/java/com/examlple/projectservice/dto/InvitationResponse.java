package com.examlple.projectservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class InvitationResponse {
    private String email;
    private String message;
    private boolean userExists;
}
