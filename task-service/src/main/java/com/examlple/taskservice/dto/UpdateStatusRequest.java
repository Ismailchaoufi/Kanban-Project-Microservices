package com.examlple.taskservice.dto;

import com.examlple.taskservice.entity.TaskStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateStatusRequest {

    @NotNull(message = "Status ID is required")
    private Long statusId;

    // Position dans la nouvelle colonne (optionnel)
    private Integer position;
}
