package com.examlple.projectservice.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskStatsDTO {
    private Long projectId;
    private Integer totalTasks;
    private Integer todoTasks;
    private Integer inProgressTasks;
    private Integer doneTasks;
}
