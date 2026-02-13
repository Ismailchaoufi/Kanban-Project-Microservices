package com.examlple.taskservice.service;

import com.examlple.taskservice.client.ProjectServiceClient;
import com.examlple.taskservice.dto.ProjectDTO;
import com.examlple.taskservice.dto.TaskStatusRequest;
import com.examlple.taskservice.dto.TaskStatusResponse;
import com.examlple.taskservice.entity.Task;
import com.examlple.taskservice.entity.TaskStatusEntity;
import com.examlple.taskservice.exception.BadRequestException;
import com.examlple.taskservice.exception.ForbiddenException;
import com.examlple.taskservice.exception.ResourceNotFoundException;
import com.examlple.taskservice.repository.TaskRepository;
import com.examlple.taskservice.repository.TaskStatusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskStatusService {
    private final TaskStatusRepository statusRepository;
    private final TaskRepository taskRepository;
    private final ProjectServiceClient projectServiceClient;

    /**
     * Initialize default statuses for a new project
     */
    @Transactional
    public void initializeDefaultStatuses(Long projectId) {
        // Create default statuses: TODO, IN_PROGRESS, DONE
        createDefaultStatus(projectId, "To Do", "#ff9800", 0);
        createDefaultStatus(projectId, "In Progress", "#2196f3", 1);
        createDefaultStatus(projectId, "Done", "#4caf50", 2);

        log.info("Initialized default statuses for project {}", projectId);
    }

    private void createDefaultStatus(Long projectId, String name, String color, int position) {
        TaskStatusEntity status = new TaskStatusEntity();
        status.setProjectId(projectId);
        status.setName(name);
        status.setColor(color);
        status.setPosition(position);
        status.setIsDefault(true);
        statusRepository.save(status);
    }

    /**
     * Get all statuses for a project
     */
    @Transactional(readOnly = true)
    public List<TaskStatusResponse> getProjectStatuses(Long projectId, Long userId, String role) {
        verifyProjectAccess(projectId, userId, role);

        List<TaskStatusEntity> statuses = statusRepository.findByProjectIdOrderByPositionAsc(projectId);

        return statuses.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Create a new custom status
     */
    @Transactional
    public TaskStatusResponse createStatus(Long projectId, TaskStatusRequest request, Long userId, String role) {
        ProjectDTO project = verifyProjectAccess(projectId, userId, role);

        // Only project owner or admin can create statuses
        if (!project.getOwnerId().equals(userId) && !"ADMIN".equals(role)) {
            throw new ForbiddenException("Only project owner can create custom statuses");
        }

        // Check if status name already exists
        if (statusRepository.existsByProjectIdAndName(projectId, request.getName())) {
            throw new BadRequestException("Status with name '" + request.getName() + "' already exists");
        }

        // Get next position
        Integer maxPosition = statusRepository.findMaxPositionByProjectId(projectId);
        Integer position = request.getPosition() != null ? request.getPosition() : maxPosition + 1;

        TaskStatusEntity status = new TaskStatusEntity();
        status.setProjectId(projectId);
        status.setName(request.getName());
        status.setColor(request.getColor());
        status.setPosition(position);
        status.setIsDefault(false);

        TaskStatusEntity savedStatus = statusRepository.save(status);
        log.info("Created new status '{}' for project {}", request.getName(), projectId);

        return mapToResponse(savedStatus);
    }

    /**
     * Update a status
     */
    @Transactional
    public TaskStatusResponse updateStatus(Long statusId, TaskStatusRequest request, Long userId, String role) {
        TaskStatusEntity status = statusRepository.findById(statusId)
                .orElseThrow(() -> new ResourceNotFoundException("Status not found"));

        ProjectDTO project = verifyProjectAccess(status.getProjectId(), userId, role);

        // Only project owner or admin can update statuses
        if (!project.getOwnerId().equals(userId) && !"ADMIN".equals(role)) {
            throw new ForbiddenException("Only project owner can update statuses");
        }

        // Check if new name conflicts with existing status
        if (!status.getName().equals(request.getName())) {
            if (statusRepository.existsByProjectIdAndName(status.getProjectId(), request.getName())) {
                throw new BadRequestException("Status with name '" + request.getName() + "' already exists");
            }
        }

        status.setName(request.getName());
        status.setColor(request.getColor());

        TaskStatusEntity updatedStatus = statusRepository.save(status);
        log.info("Updated status {} to '{}'", statusId, request.getName());

        return mapToResponse(updatedStatus);
    }

    /**
     * Delete a status (and move tasks to another status)
     */
    @Transactional
    public void deleteStatus(Long statusId, Long moveToStatusId, Long userId, String role) {
        TaskStatusEntity status = statusRepository.findById(statusId)
                .orElseThrow(() -> new ResourceNotFoundException("Status not found"));

        ProjectDTO project = verifyProjectAccess(status.getProjectId(), userId, role);

        // Only project owner or admin can delete statuses
        if (!project.getOwnerId().equals(userId) && !"ADMIN".equals(role)) {
            throw new ForbiddenException("Only project owner can delete statuses");
        }

        // Cannot delete if it's the only status
        long statusCount = statusRepository.countByProjectId(status.getProjectId());
        if (statusCount <= 1) {
            throw new BadRequestException("Cannot delete the last status");
        }

        // Get target status for moving tasks
        TaskStatusEntity moveToStatus = statusRepository.findById(moveToStatusId)
                .orElseThrow(() -> new ResourceNotFoundException("Target status not found"));

        if (!moveToStatus.getProjectId().equals(status.getProjectId())) {
            throw new BadRequestException("Target status must be in the same project");
        }

        // Move all tasks to the target status
        List<Task> tasksToMove = taskRepository.findByStatusOrderByPositionAsc(status);
        Integer maxPosition = taskRepository.findMaxPositionByStatus(moveToStatus);

        for (int i = 0; i < tasksToMove.size(); i++) {
            Task task = tasksToMove.get(i);
            task.setStatus(moveToStatus);
            task.setPosition(maxPosition + i + 1);
        }
        taskRepository.saveAll(tasksToMove);

        // Delete the status
        statusRepository.delete(status);
        log.info("Deleted status {} and moved {} tasks to status {}",
                statusId, tasksToMove.size(), moveToStatusId);
    }

    /**
     * Reorder statuses
     */
    @Transactional
    public List<TaskStatusResponse> reorderStatuses(Long projectId, List<Long> statusIds, Long userId, String role) {
        ProjectDTO project = verifyProjectAccess(projectId, userId, role);

        // Only project owner or admin can reorder statuses
        if (!project.getOwnerId().equals(userId) && !"ADMIN".equals(role)) {
            throw new ForbiddenException("Only project owner can reorder statuses");
        }

        // Validate all status IDs belong to this project
        List<TaskStatusEntity> statuses = statusRepository.findByProjectIdOrderByPositionAsc(projectId);
        if (statuses.size() != statusIds.size()) {
            throw new BadRequestException("Invalid status IDs provided");
        }

        // Update positions
        for (int i = 0; i < statusIds.size(); i++) {
            Long statusId = statusIds.get(i);
            TaskStatusEntity status = statuses.stream()
                    .filter(s -> s.getId().equals(statusId))
                    .findFirst()
                    .orElseThrow(() -> new BadRequestException("Status " + statusId + " not found in project"));

            status.setPosition(i);
        }

        List<TaskStatusEntity> updatedStatuses = statusRepository.saveAll(statuses);
        log.info("Reordered statuses for project {}", projectId);

        return updatedStatuses.stream()
                .sorted((a, b) -> a.getPosition().compareTo(b.getPosition()))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private ProjectDTO verifyProjectAccess(Long projectId, Long userId, String role) {
        try {
            return projectServiceClient.getProjectById(projectId, userId, role);
        } catch (Exception e) {
            log.error("Failed to verify project access for projectId: {}", projectId, e);
            throw new BadRequestException("Project not found or access denied");
        }
    }

    private TaskStatusResponse mapToResponse(TaskStatusEntity status) {
        Long taskCount = taskRepository.countByStatus(status);

        return TaskStatusResponse.builder()
                .id(status.getId())
                .name(status.getName())
                .color(status.getColor())
                .projectId(status.getProjectId())
                .position(status.getPosition())
                .isDefault(status.getIsDefault())
                .taskCount(taskCount.intValue())
                .build();
    }
}
