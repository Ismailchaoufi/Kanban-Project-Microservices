package com.example.projectservice.controller;


import com.example.projectservice.dto.ReorderStatusRequest;
import com.example.projectservice.dto.TaskStatusResponse;
import com.example.projectservice.service.TaskStatusService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/v1/projects/{projectId}/statuses")
@RequiredArgsConstructor
public class TaskStatusController {

    private final TaskStatusService taskStatusService;

    /**
     * GET /api/v1/projects/{projectId}/statuses
     * Get all statuses for a project (ordered by position)
     */
    @GetMapping
    public ResponseEntity<List<TaskStatusResponse>> getProjectStatuses(
            @PathVariable Long projectId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role) {

        List<TaskStatusResponse> statuses = taskStatusService.getProjectStatuses(projectId, userId, role);
        return ResponseEntity.ok(statuses);
    }

    /**
     * GET /api/v1/projects/{projectId}/statuses/{statusId}
     * Get a single status by ID
     */
    @GetMapping("/{statusId}")
    public ResponseEntity<TaskStatusResponse> getStatusById(
            @PathVariable Long projectId,
            @PathVariable Long statusId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role) {

        TaskStatusResponse status = taskStatusService.getStatusById(projectId, statusId, userId, role);
        return ResponseEntity.ok(status);
    }

    /**
     * POST /api/v1/projects/{projectId}/statuses
     * Create a new custom status
     *
     * Example request body:
     * {
     *   "name": "Code Review",
     *   "color": "#9c27b0"
     * }
     */
    @PostMapping
    public ResponseEntity<TaskStatusResponse> createStatus(
            @PathVariable Long projectId,
            @Valid @RequestBody com.example.projectservice.dto.TaskStatusRequest request,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role) {

        TaskStatusResponse response = taskStatusService.createStatus(projectId, request, userId, role);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * PUT /api/v1/projects/{projectId}/statuses/{statusId}
     * Update a status (name and/or color)
     *
     * Example request body:
     * {
     *   "name": "Peer Review",
     *   "color": "#e91e63"
     * }
     */
    @PutMapping("/{statusId}")
    public ResponseEntity<TaskStatusResponse> updateStatus(
            @PathVariable Long projectId,
            @PathVariable Long statusId,
            @Valid @RequestBody com.example.projectservice.dto.TaskStatusRequest request,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role) {

        TaskStatusResponse response = taskStatusService.updateStatus(projectId, statusId, request, userId, role);
        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /api/v1/projects/{projectId}/statuses/{statusId}
     * Delete a status and move its tasks to another status
     *
     * Query param moveToStatusId is required - specifies where to move tasks
     *
     * Example: DELETE /api/v1/projects/1/statuses/5?moveToStatusId=2
     */
    @DeleteMapping("/{statusId}")
    public ResponseEntity<Void> deleteStatus(
            @PathVariable Long projectId,
            @PathVariable Long statusId,
            @RequestParam Long moveToStatusId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role) {

        taskStatusService.deleteStatus(projectId, statusId, moveToStatusId, userId, role);
        return ResponseEntity.noContent().build();
    }

    /**
     * PUT /api/v1/projects/{projectId}/statuses/reorder
     * Reorder statuses (for drag and drop columns)
     *
     * Example request body:
     * {
     *   "statusIds": [2, 1, 3, 4]
     * }
     */
    @PutMapping("/reorder")
    public ResponseEntity<List<TaskStatusResponse>> reorderStatuses(
            @PathVariable Long projectId,
            @Valid @RequestBody ReorderStatusRequest request,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role) {

        List<TaskStatusResponse> statuses = taskStatusService.reorderStatuses(
                projectId, request.getStatusIds(), userId, role);
        return ResponseEntity.ok(statuses);
    }
}