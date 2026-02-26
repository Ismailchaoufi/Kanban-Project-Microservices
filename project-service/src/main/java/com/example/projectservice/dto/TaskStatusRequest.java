package com.example.projectservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating or updating a task status
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskStatusRequest {

    @NotBlank(message = "Status name is required")
    private String name;

    @NotBlank(message = "Color is required")
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Color must be a valid hex color (e.g., #ff9800)")
    private String color;

    private Integer position;
}