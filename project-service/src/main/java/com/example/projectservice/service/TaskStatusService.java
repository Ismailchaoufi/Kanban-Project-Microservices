package com.example.projectservice.service;

import com.example.projectservice.dto.TaskStatusRequest;
import com.example.projectservice.dto.TaskStatusResponse;
import com.example.projectservice.entity.Project;
import com.example.projectservice.entity.TaskStatusEntity;
import com.example.projectservice.exception.BadRequestException;
import com.example.projectservice.exception.ForbiddenException;
import com.example.projectservice.exception.ResourceNotFoundException;
import com.example.projectservice.repository.ProjectRepository;
import com.example.projectservice.repository.TaskStatusRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing task statuses (Kanban columns)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TaskStatusService {

    private final TaskStatusRepository statusRepository;
    private final ProjectRepository projectRepository;

    /**
     * Initialize default statuses for a new project
     * Called automatically when a project is created
     */
    @Transactional
    public void initializeDefaultStatuses(Long projectId) {
        log.info("Initializing default statuses for project {}", projectId);

        // Create 3 default statuses like Trello
        createDefaultStatus(projectId, "To Do", "#ff9800", 0);
        createDefaultStatus(projectId, "In Progress", "#2196f3", 1);
        createDefaultStatus(projectId, "Done", "#4caf50", 2);

        log.info("Default statuses created for project {}", projectId);
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
     * Get all statuses for a project (ordered by position)
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
     * Get a single status by ID
     */
    @Transactional(readOnly = true)
    public TaskStatusResponse getStatusById(Long projectId, Long statusId, Long userId, String role) {
        verifyProjectAccess(projectId, userId, role);

        TaskStatusEntity status = statusRepository.findByIdAndProjectId(statusId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Status not found"));

        return mapToResponse(status);
    }

    /**
     * Create a new custom status
     */
    @Transactional
    public TaskStatusResponse createStatus(Long projectId, TaskStatusRequest request, Long userId, String role) {
        Project project = verifyProjectAccess(projectId, userId, role);

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
        log.info("Created custom status '{}' for project {}", request.getName(), projectId);

        return mapToResponse(savedStatus);
    }

    /**
     * Update a status (name and/or color)
     */
    @Transactional
    public TaskStatusResponse updateStatus(Long projectId, Long statusId, TaskStatusRequest request,
                                           Long userId, String role) {
        TaskStatusEntity status = statusRepository.findByIdAndProjectId(statusId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Status not found"));

        Project project = verifyProjectAccess(projectId, userId, role);

        // Only project owner or admin can update statuses
        if (!project.getOwnerId().equals(userId) && !"ADMIN".equals(role)) {
            throw new ForbiddenException("Only project owner can update statuses");
        }

        // Check if new name conflicts with existing status
        if (!status.getName().equals(request.getName())) {
            if (statusRepository.existsByProjectIdAndName(projectId, request.getName())) {
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
     * Delete a status
     * Note: In microservices, Task Service will need to handle moving tasks
     */
    @Transactional
    public void deleteStatus(Long projectId, Long statusId, Long moveToStatusId, Long userId, String role) {
        TaskStatusEntity status = statusRepository.findByIdAndProjectId(statusId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Status not found"));

        Project project = verifyProjectAccess(projectId, userId, role);

        // Only project owner or admin can delete statuses
        if (!project.getOwnerId().equals(userId) && !"ADMIN".equals(role)) {
            throw new ForbiddenException("Only project owner can delete statuses");
        }

        // Cannot delete if it's the only status
        long statusCount = statusRepository.countByProjectId(projectId);
        if (statusCount <= 1) {
            throw new BadRequestException("Cannot delete the last status");
        }

        // Validate moveToStatus exists and belongs to same project
        TaskStatusEntity moveToStatus = statusRepository.findByIdAndProjectId(moveToStatusId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Target status not found"));

        if (moveToStatus.getId().equals(statusId)) {
            throw new BadRequestException("Cannot move tasks to the same status being deleted");
        }

        // Delete the status
        statusRepository.delete(status);

        // TODO: Publish event for Task Service to migrate tasks
        // Example: eventPublisher.publishEvent(new StatusDeletedEvent(statusId, moveToStatusId, projectId));

        log.info("Deleted status {} from project {}, tasks should move to status {}",
                statusId, projectId, moveToStatusId);
    }

    /**
     * Reorder statuses (drag and drop columns)
     */
    @Transactional
    public List<TaskStatusResponse> reorderStatuses(Long projectId, List<Long> statusIds,
                                                    Long userId, String role) {
        Project project = verifyProjectAccess(projectId, userId, role);

        // Only project owner or admin can reorder statuses
        if (!project.getOwnerId().equals(userId) && !"ADMIN".equals(role)) {
            throw new ForbiddenException("Only project owner can reorder statuses");
        }

        // Get all statuses for the project
        List<TaskStatusEntity> statuses = statusRepository.findByProjectIdOrderByPositionAsc(projectId);

        // Validate all status IDs belong to this project
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

    /**
     * Verify user has access to project
     */
    private Project verifyProjectAccess(Long projectId, Long userId, String role) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        // Add your access control logic here
        // For example: check if user is owner, member, or admin
        // This is a simplified version - adjust based on your security model

        return project;
    }

    /**
     * Map entity to response DTO
     */
    private TaskStatusResponse mapToResponse(TaskStatusEntity status) {
        return TaskStatusResponse.builder()
                .id(status.getId())
                .name(status.getName())
                .color(status.getColor())
                .projectId(status.getProjectId())
                .position(status.getPosition())
                .isDefault(status.getIsDefault())
                .taskCount(0) // Task Service will provide this if needed
                .build();
    }
}