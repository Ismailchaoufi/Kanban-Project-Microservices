package com.examlple.taskservice.controller;

import com.examlple.taskservice.dto.TaskRequest;
import com.examlple.taskservice.dto.TaskResponse;
import com.examlple.taskservice.dto.TaskStatsResponse;
import com.examlple.taskservice.dto.UpdateStatusRequest;
import com.examlple.taskservice.entity.Priority;
import com.examlple.taskservice.entity.TaskStatus;
import com.examlple.taskservice.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    public ResponseEntity<TaskResponse> createTask(
            @Valid @RequestBody TaskRequest request,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role) {
        TaskResponse response = taskService.createTask(request, userId, role);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<TaskResponse>> getAllTasks(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Long statusId,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) Long assignedTo,
            @RequestParam(required = false) String search,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role,
            Pageable pageable) {
        Page<TaskResponse> tasks = taskService.getAllTasks(
                projectId, statusId, priority, assignedTo, search, userId, role, pageable);
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskResponse> getTaskById(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role) {
        TaskResponse task = taskService.getTaskById(id, userId, role);
        return ResponseEntity.ok(task);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaskResponse> updateTask(
            @PathVariable Long id,
            @Valid @RequestBody TaskRequest request,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role) {
        TaskResponse task = taskService.updateTask(id, request, userId, role);
        return ResponseEntity.ok(task);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<TaskResponse> updateTaskStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequest request,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role) {
        TaskResponse task = taskService.updateTaskStatus(id, request, userId, role);
        return ResponseEntity.ok(task);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role) {
        taskService.deleteTask(id, userId, role);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats")
    public ResponseEntity<TaskStatsResponse> getTaskStats(
            @RequestParam Long projectId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role) {
        TaskStatsResponse stats = taskService.getTaskStatsByProject(projectId, userId, role);
        return ResponseEntity.ok(stats);
    }
}
