package com.examlple.taskservice.service;

import com.examlple.taskservice.client.AuthServiceClient;
import com.examlple.taskservice.client.ProjectServiceClient;
import com.examlple.taskservice.dto.*;
import com.examlple.taskservice.entity.Priority;
import com.examlple.taskservice.entity.Task;
import com.examlple.taskservice.entity.TaskStatus;
import com.examlple.taskservice.exception.BadRequestException;
import com.examlple.taskservice.exception.ForbiddenException;
import com.examlple.taskservice.exception.ResourceNotFoundException;
import com.examlple.taskservice.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectServiceClient projectServiceClient;
    private final AuthServiceClient authServiceClient;

    /**
     * Create a new task only if:
     * - The project exists
     * - The user has access to the project
     * - The assigned user exists (if provided)
     */
    @Transactional
    public TaskResponse createTask(TaskRequest request, Long userId, String role) {
        // Verify that the project exists and user has access
        verifyProjectAccess(request.getProjectId(), userId, role);

        // Verify that the assigned user exists
        if (request.getAssignedTo() != null) {
            verifyUserExists(request.getAssignedTo(), userId, role);
        }

        Task task = new Task();
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setStatus(request.getStatus() != null ? request.getStatus() : TaskStatus.TODO);
        task.setPriority(request.getPriority() != null ? request.getPriority() : Priority.MEDIUM);
        task.setDueDate(request.getDueDate());
        task.setProjectId(request.getProjectId());
        task.setAssignedTo(request.getAssignedTo());

        Task savedTask = taskRepository.save(task);
        log.info("Task created successfully with ID: {}", savedTask.getId());

        return mapToTaskResponse(savedTask, userId, role);
    }

    /**
     * Retrieve tasks with dynamic filters:
     * - By project
     * - By status
     * - By priority
     * - By assigned user
     * - Text search
     * - Pagination
     */
    @Transactional(readOnly = true)
    public Page<TaskResponse> getAllTasks(Long projectId, TaskStatus status, Priority priority,
                                          Long assignedTo, String search, Long userId, String role,
                                          Pageable pageable) {
        Page<Task> tasks;

        if (projectId != null) {
            // Verify user has access to project
            verifyProjectAccess(projectId, userId, role);

            if (search != null && !search.isEmpty()) {
                tasks = taskRepository.searchTasksByProject(projectId, search, pageable);
            } else if (status != null) {
                tasks = taskRepository.findByProjectIdAndStatus(projectId, status, pageable);
            } else if (assignedTo != null) {
                tasks = taskRepository.findByProjectIdAndAssignedTo(projectId, assignedTo, pageable);
            } else {
                tasks = taskRepository.findByProjectId(projectId, pageable);
            }
        } else if (assignedTo != null) {
            tasks = taskRepository.findByAssignedTo(assignedTo, pageable);
        } else if (status != null) {
            tasks = taskRepository.findByStatus(status, pageable);
        } else if (priority != null) {
            tasks = taskRepository.findByPriority(priority, pageable);
        } else {
            // Admin can see all tasks
            if ("ADMIN".equals(role)) {
                tasks = taskRepository.findAll(pageable);
            } else {
                throw new BadRequestException("Must specify projectId or assignedTo");
            }
        }

        return tasks.map(task -> mapToTaskResponse(task, userId, role));
    }

    /**
     * Retrieve a task by ID if the user has access to the project
     */
    @Transactional(readOnly = true)
    public TaskResponse getTaskById(Long taskId, Long userId, String role) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));

        // Verify user has access
        verifyProjectAccess(task.getProjectId(), userId, role);

        return mapToTaskResponse(task, userId, role);
    }

    /**
     * Update a task (partial update)
     */
    @Transactional
    public TaskResponse updateTask(Long taskId, TaskRequest request, Long userId, String role) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));

        // Verify access to project
        verifyProjectAccess(task.getProjectId(), userId, role);

        // Verify assigned user
        if (request.getAssignedTo() != null) {
            verifyUserExists(request.getAssignedTo(), userId, role);
        }

        // Update fields one by one
        if (request.getTitle() != null) {
            task.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            task.setDescription(request.getDescription());
        }
        if (request.getStatus() != null) {
            task.setStatus(request.getStatus());
        }
        if (request.getPriority() != null) {
            task.setPriority(request.getPriority());
        }
        if (request.getDueDate() != null) {
            task.setDueDate(request.getDueDate());
        }
        if (request.getAssignedTo() != null) {
            task.setAssignedTo(request.getAssignedTo());
        }

        Task updatedTask = taskRepository.save(task);
        log.info("Task {} updated successfully", taskId);

        return mapToTaskResponse(updatedTask, userId, role);
    }

    /**
     * Change only the status of a task
     * Kanban (drag & drop)
     */
    @Transactional
    public TaskResponse updateTaskStatus(Long taskId, UpdateStatusRequest request, Long userId, String role) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));

        verifyProjectAccess(task.getProjectId(), userId, role);

        task.setStatus(request.getStatus());
        Task updatedTask = taskRepository.save(task);

        log.info("Task {} status updated to {}", taskId, request.getStatus());

        return mapToTaskResponse(updatedTask, userId, role);
    }

    /**
     * Delete a task
     * Only the project owner or ADMIN
     */
    @Transactional
    public void deleteTask(Long taskId, Long userId, String role) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));

        ProjectDTO project = verifyProjectAccess(task.getProjectId(), userId, role);

        if (!project.getOwnerId().equals(userId) && !"ADMIN".equals(role)) {
            throw new ForbiddenException("Only the project owner can delete tasks");
        }

        taskRepository.delete(task);
        log.info("Task {} deleted successfully", taskId);
    }

    /**
     * Kanban statistics by project (dashboard)
     */
    @Transactional(readOnly = true)
    public TaskStatsResponse getTaskStatsByProject(Long projectId, Long userId, String role) {
        verifyProjectAccess(projectId, userId, role);

        Long totalTasks = taskRepository.countByProjectId(projectId);
        Long todoTasks = taskRepository.countByProjectIdAndStatus(projectId, TaskStatus.TODO);
        Long inProgressTasks = taskRepository.countByProjectIdAndStatus(projectId, TaskStatus.IN_PROGRESS);
        Long doneTasks = taskRepository.countByProjectIdAndStatus(projectId, TaskStatus.DONE);

        return TaskStatsResponse.builder()
                .projectId(projectId)
                .totalTasks(totalTasks.intValue())
                .todoTasks(todoTasks.intValue())
                .inProgressTasks(inProgressTasks.intValue())
                .doneTasks(doneTasks.intValue())
                .build();
    }

    /**
     * Verify access to project
     */
    private ProjectDTO verifyProjectAccess(Long projectId, Long userId, String role) {
        try {
            return projectServiceClient.getProjectById(projectId, userId, role);
        } catch (Exception e) {
            log.error("Failed to verify project access for projectId: {}", projectId, e);
            throw new BadRequestException("Project not found or access denied");
        }
    }

    /**
     * Verify that a user exists
     */
    private void verifyUserExists(Long userIdToVerify, Long requesterId, String role) {
        try {
            authServiceClient.getUserById(userIdToVerify, requesterId, role);
        } catch (Exception e) {
            log.error("Failed to verify user exists for userId: {}", userIdToVerify, e);
            throw new BadRequestException("User not found");
        }
    }

    /**
     * Map Task entity to TaskResponse DTO
     */
    private TaskResponse mapToTaskResponse(Task task, Long userId, String role) {
        AssignedUserDTO assignedUser = null;

        if (task.getAssignedTo() != null) {
            try {
                UserDTO user = authServiceClient.getUserById(task.getAssignedTo(), userId, role);
                assignedUser = AssignedUserDTO.builder()
                        .id(user.getId())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .email(user.getEmail())
                        .avatarUrl(user.getAvatarUrl())
                        .build();
            } catch (Exception e) {
                log.error("Failed to fetch assigned user details for userId: {}", task.getAssignedTo(), e);
            }
        }

        return TaskResponse.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus())
                .priority(task.getPriority())
                .dueDate(task.getDueDate())
                .projectId(task.getProjectId())
                .assignedUser(assignedUser)
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }
}
