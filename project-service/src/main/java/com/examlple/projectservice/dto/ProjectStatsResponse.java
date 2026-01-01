package com.examlple.projectservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectStatsResponse {
    private Long projectId;
    private String projectTitle;
    private Integer totalTasks;
    private Integer todoTasks;
    private Integer inProgressTasks;
    private Integer doneTasks;
    private Integer totalMembers;
}
