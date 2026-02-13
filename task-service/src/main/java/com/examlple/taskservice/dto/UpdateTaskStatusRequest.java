package com.examlple.taskservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class UpdateTaskStatusRequest {
    @NotNull(message = "Status ID is required")
    private Long statusId;

    private Integer position;
}
