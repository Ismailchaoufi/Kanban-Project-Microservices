package com.examlple.taskservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskStatsResponse {
    private Long projectId;
    private Integer totalTasks;
    private Integer todoTasks;
    private Integer inProgressTasks;
    private Integer doneTasks;
}
