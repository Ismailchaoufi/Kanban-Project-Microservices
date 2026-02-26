package com.example.projectservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskStatusResponse {
    private Long id;
    private String name;
    private String color;
    private Long projectId;
    private Integer position;
    private Boolean isDefault;
    private Integer taskCount;
}
