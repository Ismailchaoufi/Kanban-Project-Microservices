package com.examlple.projectservice.client;

import com.examlple.projectservice.entity.TaskStatsDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "TASK-SERVICE")
public interface TaskServiceClient {

    @GetMapping("/api/v1/tasks/stats")
    TaskStatsDTO getTaskStatsByProject(@RequestParam("projectId") Long projectId);
}
