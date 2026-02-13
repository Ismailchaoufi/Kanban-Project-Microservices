package com.examlple.taskservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class ReorderStatusRequest {
    @NotNull(message = "Status IDs are required")
    private List<Long> statusIds;
}
