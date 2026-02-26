package com.example.taskservice.client;

import com.example.taskservice.dto.ProjectDTO;
import com.example.taskservice.dto.StatusDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

@FeignClient(name = "PROJECT-SERVICE")
public interface ProjectServiceClient {

    /**
     * Get a single status by ID
     * Used to validate status belongs to project when creating/updating tasks
     */
    @GetMapping("/api/v1/projects/{projectId}/statuses/{statusId}")
    StatusDTO getStatusById(
            @PathVariable("projectId") Long projectId,
            @PathVariable("statusId") Long statusId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role
    );

    /**
     * Get all statuses for a project
     * Used to enrich task responses with status details
     */
    @GetMapping("/api/v1/projects/{projectId}/statuses")
    List<StatusDTO> getProjectStatuses(
            @PathVariable("projectId") Long projectId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role
    );

    @GetMapping("/api/v1/projects/{id}")
    ProjectDTO getProjectById(
            @PathVariable("id") Long id,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role
    );
}