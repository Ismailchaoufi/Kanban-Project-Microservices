package com.examlple.taskservice.client;

import com.examlple.taskservice.dto.ProjectDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "PROJECT-SERVICE")
public interface ProjectServiceClient {

    @GetMapping("/api/v1/projects/{id}")
    ProjectDTO getProjectById(
            @PathVariable("id") Long id,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role
    );
}