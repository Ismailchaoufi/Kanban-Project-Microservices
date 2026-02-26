package com.example.taskservice.dto;

import com.example.taskservice.entity.Priority;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResponse {
    private Long id;
    private String title;
    private String description;
    private StatusDTO status;
    private Priority priority;
    private LocalDate dueDate;
    private Long projectId;
    private Long assignedUser;
    private Integer position;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
