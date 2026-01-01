package com.examlple.projectservice.dto;

import com.examlple.projectservice.entity.ProjectStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ProjectRequest {

    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 100, message = "Title must be between 3 and 100 characters")
    private String title;

    private String description;

    private ProjectStatus status;

    private String color;

    private LocalDate startDate;

    private LocalDate endDate;
}
