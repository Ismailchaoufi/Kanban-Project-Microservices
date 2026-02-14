package com.examlple.taskservice.service;

import com.examlple.taskservice.client.AuthServiceClient;
import com.examlple.taskservice.client.ProjectServiceClient;
import com.examlple.taskservice.dto.*;
import com.examlple.taskservice.entity.Priority;
import com.examlple.taskservice.entity.Task;
import com.examlple.taskservice.entity.TaskStatusEntity;
import com.examlple.taskservice.exception.BadRequestException;
import com.examlple.taskservice.exception.ForbiddenException;
import com.examlple.taskservice.exception.ResourceNotFoundException;
import com.examlple.taskservice.repository.TaskRepository;
import com.examlple.taskservice.repository.TaskStatusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskStatusRepository taskStatusRepository;  // NOUVEAU
    private final ProjectServiceClient projectServiceClient;
    private final AuthServiceClient authServiceClient;

    /**
     * Créer une nouvelle tâche uniquement si :
     * - Le projet existe
     * - L'utilisateur a accès au projet
     * - L'utilisateur assigné existe (si fourni)
     * - Le statut existe et appartient au projet
     */
    @Transactional
    public TaskResponse createTask(TaskRequest request, Long userId, String role) {
        // Vérifie que le projet existe + que l'utilisateur y a accès
        verifyProjectAccess(request.getProjectId(), userId, role);

        // Vérifie que l'utilisateur assigné existe
        if (request.getAssignedTo() != null) {
            verifyUserExists(request.getAssignedTo(), userId, role);
        }

        // NOUVEAU: Récupérer le statut ou utiliser le premier statut du projet par défaut
        TaskStatusEntity status;
        if (request.getStatusId() != null) {
            status = taskStatusRepository.findByIdAndProjectId(request.getStatusId(), request.getProjectId())
                    .orElseThrow(() -> new BadRequestException("Status not found or does not belong to this project"));
        } else {
            // Si aucun statut n'est fourni, utiliser le premier statut du projet (position 0)
            List<TaskStatusEntity> statuses = taskStatusRepository.findByProjectIdOrderByPositionAsc(request.getProjectId());
            if (statuses.isEmpty()) {
                throw new BadRequestException("No statuses found for this project. Please create statuses first.");
            }
            status = statuses.get(0);
        }

        // Calculer la position dans la colonne
        Integer maxPosition = taskRepository.findMaxPositionByStatus(status);
        Integer position = (maxPosition != null) ? maxPosition + 1 : 0;

        Task task = new Task();
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setStatus(status);  // CHANGÉ: maintenant c'est une entité
        task.setPriority(request.getPriority() != null ? request.getPriority() : Priority.MEDIUM);
        task.setDueDate(request.getDueDate());
        task.setProjectId(request.getProjectId());
        task.setAssignedTo(request.getAssignedTo());
        task.setPosition(position);  // NOUVEAU

        Task savedTask = taskRepository.save(task);
        log.info("Task created successfully with ID: {} in status: {}", savedTask.getId(), status.getName());

        return mapToTaskResponse(savedTask, userId, role);
    }

    /**
     * Récupérer les tâches avec filtres dynamiques :
     * - Par projet
     * - Par statut (maintenant par statusId)
     * - Par priorité
     * - Par utilisateur assigné
     * - Recherche texte
     * - Pagination
     */
    @Transactional(readOnly = true)
    public Page<TaskResponse> getAllTasks(Long projectId, Long statusId, Priority priority,
                                          Long assignedTo, String search, Long userId, String role,
                                          Pageable pageable) {
        Page<Task> tasks;

        if (projectId != null) {
            // Verify user has access to project
            verifyProjectAccess(projectId, userId, role);

            if (search != null && !search.isEmpty()) {
                tasks = taskRepository.searchTasksByProject(projectId, search, pageable);
            } else if (statusId != null) {
                // CHANGÉ: Récupérer le statut par ID
                TaskStatusEntity status = taskStatusRepository.findByIdAndProjectId(statusId, projectId)
                        .orElseThrow(() -> new BadRequestException("Status not found"));
                tasks = taskRepository.findByProjectIdAndStatus(projectId, status, pageable);
            } else if (assignedTo != null) {
                tasks = taskRepository.findByProjectIdAndAssignedTo(projectId, assignedTo, pageable);
            } else {
                tasks = taskRepository.findByProjectId(projectId, pageable);
            }
        } else if (assignedTo != null) {
            tasks = taskRepository.findByAssignedTo(assignedTo, pageable);
        } else if (statusId != null) {
            // CHANGÉ: Récupérer le statut par ID
            TaskStatusEntity status = taskStatusRepository.findById(statusId)
                    .orElseThrow(() -> new BadRequestException("Status not found"));
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
     * Récupérer une tâche par ID si l'utilisateur a accès au projet
     */
    @Transactional(readOnly = true)
    public TaskResponse getTaskById(Long taskId, Long userId, String role) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));

        verifyProjectAccess(task.getProjectId(), userId, role);

        return mapToTaskResponse(task, userId, role);
    }

    /**
     * Modifier une tâche (mise à jour partielle)
     */
    @Transactional
    public TaskResponse updateTask(Long taskId, TaskRequest request, Long userId, String role) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));

        // Vérifie accès au projet
        verifyProjectAccess(task.getProjectId(), userId, role);

        // Vérifie l'utilisateur assigné
        if (request.getAssignedTo() != null) {
            verifyUserExists(request.getAssignedTo(), userId, role);
        }

        // Mise à jour champ par champ
        if (request.getTitle() != null) {
            task.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            task.setDescription(request.getDescription());
        }
        if (request.getStatusId() != null) {
            // CHANGÉ: Récupérer le statut par ID
            TaskStatusEntity newStatus = taskStatusRepository.findByIdAndProjectId(
                            request.getStatusId(), task.getProjectId())
                    .orElseThrow(() -> new BadRequestException("Status not found or does not belong to this project"));

            // Si le statut change, recalculer la position
            if (!task.getStatus().getId().equals(newStatus.getId())) {
                Integer maxPosition = taskRepository.findMaxPositionByStatus(newStatus);
                task.setPosition((maxPosition != null) ? maxPosition + 1 : 0);
            }

            task.setStatus(newStatus);
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
     * Changer uniquement le statut d'une tâche
     * Utilisé pour le Kanban (drag & drop)
     */
    @Transactional
    public TaskResponse updateTaskStatus(Long taskId, UpdateStatusRequest request, Long userId, String role) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));

        verifyProjectAccess(task.getProjectId(), userId, role);

        // CHANGÉ: Récupérer le nouveau statut par ID
        TaskStatusEntity newStatus = taskStatusRepository.findByIdAndProjectId(
                        request.getStatusId(), task.getProjectId())
                .orElseThrow(() -> new BadRequestException("Status not found or does not belong to this project"));

        // Mettre à jour le statut et la position
        task.setStatus(newStatus);

        if (request.getPosition() != null) {
            task.setPosition(request.getPosition());
        } else {
            // Si aucune position n'est fournie, mettre à la fin de la colonne
            Integer maxPosition = taskRepository.findMaxPositionByStatus(newStatus);
            task.setPosition((maxPosition != null) ? maxPosition + 1 : 0);
        }

        Task updatedTask = taskRepository.save(task);

        log.info("Task {} status updated to {} (position: {})",
                taskId, newStatus.getName(), task.getPosition());

        return mapToTaskResponse(updatedTask, userId, role);
    }

    /**
     * Supprimer une tâche
     * Seulement le owner du projet ou ADMIN
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
     * Statistiques Kanban par projet (dashboard)
     * CHANGÉ: Maintenant dynamique basé sur les statuts du projet
     */
    @Transactional(readOnly = true)
    public TaskStatsResponse getTaskStatsByProject(Long projectId, Long userId, String role) {
        verifyProjectAccess(projectId, userId, role);

        Long totalTasks = taskRepository.countByProjectId(projectId);

        // Récupérer tous les statuts du projet
        List<TaskStatusEntity> statuses = taskStatusRepository.findByProjectIdOrderByPositionAsc(projectId);

        // Compter les tâches pour chaque statut
        TaskStatsResponse.TaskStatsResponseBuilder statsBuilder = TaskStatsResponse.builder()
                .projectId(projectId)
                .totalTasks(totalTasks.intValue());

        // Pour compatibilité avec l'ancien système, on garde todoTasks, inProgressTasks, doneTasks
        // mais on les récupère dynamiquement
        for (TaskStatusEntity status : statuses) {
            Long count = taskRepository.countByProjectIdAndStatus(projectId, status);

            // Mapping pour compatibilité (basé sur les noms de statuts par défaut)
            if (status.getName().equalsIgnoreCase("To Do") || status.getName().equalsIgnoreCase("TODO")) {
                statsBuilder.todoTasks(count.intValue());
            } else if (status.getName().equalsIgnoreCase("In Progress") || status.getName().equalsIgnoreCase("IN_PROGRESS")) {
                statsBuilder.inProgressTasks(count.intValue());
            } else if (status.getName().equalsIgnoreCase("Done") || status.getName().equalsIgnoreCase("DONE")) {
                statsBuilder.doneTasks(count.intValue());
            }
        }

        return statsBuilder.build();
    }

    /**
     * Vérifier accès au projet
     */
    private ProjectDTO verifyProjectAccess(Long projectId, Long userId, String role) {
        try {
            return projectServiceClient.getProjectById(projectId, userId, role);
        } catch (Exception e) {
            log.error("Failed to verify project access for projectId: {}", projectId, e);
            throw new BadRequestException("Project not found or access denied");
        }
    }

    private void verifyUserExists(Long userIdToVerify, Long requesterId, String role) {
        try {
            authServiceClient.getUserById(userIdToVerify, requesterId, role);
        } catch (Exception e) {
            log.error("Failed to verify user exists for userId: {}", userIdToVerify, e);
            throw new BadRequestException("User not found");
        }
    }

    /**
     * Mapper Task → TaskResponse
     * CHANGÉ: Le statut est maintenant un objet avec id, name, color, etc.
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

        // CHANGÉ: Créer un StatusDTO pour la réponse
        StatusDTO statusDTO = StatusDTO.builder()
                .id(task.getStatus().getId())
                .name(task.getStatus().getName())
                .color(task.getStatus().getColor())
                .build();

        return TaskResponse.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(statusDTO)  // CHANGÉ: maintenant c'est un objet StatusDTO
                .priority(task.getPriority())
                .dueDate(task.getDueDate())
                .projectId(task.getProjectId())
                .assignedUser(assignedUser)
                .position(task.getPosition())  // NOUVEAU
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }
}