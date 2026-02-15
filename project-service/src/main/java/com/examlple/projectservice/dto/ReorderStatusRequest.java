package com.examlple.projectservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for reordering statuses (drag and drop columns)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReorderStatusRequest {

    @NotNull(message = "Status IDs are required")
    private List<Long> statusIds; // Ordered list of status IDs
}