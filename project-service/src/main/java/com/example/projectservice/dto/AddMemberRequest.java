package com.example.projectservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddMemberRequest {

    @NotNull(message = "User ID is required")
    private Long userId;
}
