package com.example.taskservice.dto;

import com.example.taskservice.entity.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class TaskRequest {

    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 100, message = "Title must be between 3 and 100 characters")
    private String title;

    private String description;

    private Long statusId;

    @NotNull(message = "Priority is required")
    private Priority priority;

    private LocalDate dueDate;

    @NotNull(message = "Project ID is required")
    private Long projectId;

    private Long assignedTo;
}
