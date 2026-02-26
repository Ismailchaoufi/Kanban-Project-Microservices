package com.example.projectservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class InvitationRequest {
    @NotBlank
    @Email
    private String email;
}