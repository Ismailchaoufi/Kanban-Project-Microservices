package com.examlple.taskservice.service;

import com.examlple.taskservice.client.ProjectServiceClient;
import com.examlple.taskservice.dto.*;
import com.examlple.taskservice.entity.Priority;
import com.examlple.taskservice.entity.Task;
import com.examlple.taskservice.exception.BadRequestException;
import com.examlple.taskservice.exception.ResourceNotFoundException;
import com.examlple.taskservice.repository.TaskRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectServiceClient projectServiceClient;

    /**
     * Créer une nouvelle tâche
     * - Valide le statut via Project Service
     * - Calcule la position automatiquement
     */
    @Transactional
    public TaskResponse createTask(TaskRequest request, Long userId, String role) {
        log.info("Creating task for project {} by user {}", request.getProjectId(), userId);

        // Valider l'accès au projet
        verifyProjectAccess(request.getProjectId(), userId, role);

        // Déterminer le statut
        StatusDTO status;
        if (request.getStatusId() != null) {
            // Valider que le statut appartient au projet
            status = validateStatus(request.getProjectId(), request.getStatusId(), userId, role);
        } else {
            // Utiliser le premier statut du projet (par défaut "To Do")
            status = getFirstProjectStatus(request.getProjectId(), userId, role);
        }

        // Créer la tâche
        Task task = new Task();
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setStatusId(status.getId());
        task.setPriority(request.getPriority() != null ? request.getPriority() : Priority.MEDIUM);
        task.setDueDate(request.getDueDate());
        task.setProjectId(request.getProjectId());
        task.setAssignedTo(request.getAssignedTo());

        // Calculer la position (à la fin de la colonne)
        Integer maxPosition = taskRepository.findMaxPositionByStatusId(status.getId());
        task.setPosition(maxPosition + 1);

        Task savedTask = taskRepository.save(task);
        log.info("Task created with ID: {}", savedTask.getId());

        return mapToTaskResponse(savedTask, status);
    }

    /**
     * Récupérer toutes les tâches avec filtres
     */
    @Transactional(readOnly = true)
    public Page<TaskResponse> getAllTasks(
            Long projectId,
            Long statusId,
            Priority priority,
            Long assignedTo,
            String search,
            Long userId,
            String role,
            Pageable pageable) {

        Page<Task> tasks;

        if (projectId != null) {
            // Valider l'accès au projet
            verifyProjectAccess(projectId, userId, role);

            // Appliquer les filtres
            if (search != null && !search.isEmpty()) {
                tasks = taskRepository.searchTasksByProject(projectId, search, pageable);
            } else if (statusId != null) {
                tasks = taskRepository.findByProjectIdAndStatusId(projectId, statusId, pageable);
            } else if (priority != null) {
                tasks = taskRepository.findByProjectIdAndPriority(projectId, priority, pageable);
            } else if (assignedTo != null) {
                tasks = taskRepository.findByProjectIdAndAssignedTo(projectId, assignedTo, pageable);
            } else {
                tasks = taskRepository.findByProjectId(projectId, pageable);
            }

            // Enrichir avec les détails des statuts
            return enrichTasksWithStatuses(tasks, projectId, userId, role);

        } else if (assignedTo != null) {
            tasks = taskRepository.findByAssignedTo(assignedTo, pageable);
        } else if (statusId != null) {
            tasks = taskRepository.findByStatusId(statusId, pageable);
        } else if (priority != null) {
            tasks = taskRepository.findByPriority(priority, pageable);
        } else {
            // Admin seulement
            if (!"ADMIN".equals(role)) {
                throw new BadRequestException("Must specify projectId or assignedTo");
            }
            tasks = taskRepository.findAll(pageable);
        }

        // Pour les requêtes non-project, enrichir individuellement
        return tasks.map(task -> {
            try {
                StatusDTO status = projectServiceClient.getStatusById(
                        task.getProjectId(),
                        task.getStatusId(),
                        userId,
                        role
                );
                return mapToTaskResponse(task, status);
            } catch (Exception e) {
                log.error("Failed to fetch status for task {}", task.getId(), e);
                return mapToTaskResponseWithoutStatus(task);
            }
        });
    }

    /**
     * Récupérer une tâche par ID
     */
    @Transactional(readOnly = true)
    public TaskResponse getTaskById(Long taskId, Long userId, String role) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));

        // Valider l'accès au projet
        verifyProjectAccess(task.getProjectId(), userId, role);

        // Récupérer les détails du statut
        StatusDTO status = fetchStatusDetails(task.getProjectId(), task.getStatusId(), userId, role);

        return mapToTaskResponse(task, status);
    }

    /**
     * Mettre à jour une tâche
     */
    @Transactional
    public TaskResponse updateTask(Long taskId, TaskRequest request, Long userId, String role) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));

        // Valider l'accès
        verifyProjectAccess(task.getProjectId(), userId, role);

        // Mise à jour des champs
        if (request.getTitle() != null) {
            task.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            task.setDescription(request.getDescription());
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

        // Si le statut change, valider et recalculer la position
        if (request.getStatusId() != null && !request.getStatusId().equals(task.getStatusId())) {
            StatusDTO newStatus = validateStatus(task.getProjectId(), request.getStatusId(), userId, role);
            task.setStatusId(newStatus.getId());

            // Mettre à la fin de la nouvelle colonne
            Integer maxPosition = taskRepository.findMaxPositionByStatusId(newStatus.getId());
            task.setPosition(maxPosition + 1);
        }

        Task updatedTask = taskRepository.save(task);
        log.info("Task {} updated", taskId);

        // Récupérer les détails du statut pour la réponse
        StatusDTO status = fetchStatusDetails(task.getProjectId(), task.getStatusId(), userId, role);

        return mapToTaskResponse(updatedTask, status);
    }

    /**
     * Mettre à jour le statut d'une tâche (drag & drop)
     */
    @Transactional
    public TaskResponse updateTaskStatus(Long taskId, UpdateTaskStatusRequest request, Long userId, String role) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));

        // Valider l'accès
        verifyProjectAccess(task.getProjectId(), userId, role);

        // Valider le nouveau statut appartient au même projet
        StatusDTO newStatus = validateStatus(task.getProjectId(), request.getStatusId(), userId, role);

        // Mettre à jour le statut et la position
        task.setStatusId(newStatus.getId());
        task.setPosition(request.getPosition() != null ? request.getPosition() : 0);

        Task updatedTask = taskRepository.save(task);
        log.info("Task {} moved to status {}", taskId, newStatus.getName());

        return mapToTaskResponse(updatedTask, newStatus);
    }

    /**
     * Supprimer une tâche
     */
    @Transactional
    public void deleteTask(Long taskId, Long userId, String role) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));

        // Valider l'accès
        verifyProjectAccess(task.getProjectId(), userId, role);

        taskRepository.delete(task);
        log.info("Task {} deleted", taskId);
    }

    /**
     * Obtenir les statistiques des tâches par projet
     * Compatible avec l'ancien format pour le Project Service
     */
    @Transactional(readOnly = true)
    public TaskStatsResponse getTaskStatsByProject(Long projectId) {
        log.info("Fetching task stats for project {}", projectId);

        // Compter le total
        Long totalTasks = taskRepository.countByProjectId(projectId);

        // Pour la compatibilité, on essaie de trouver les statuts par nom
        // Sinon retourne 0 pour chaque catégorie
        int todoTasks = 0;
        int inProgressTasks = 0;
        int doneTasks = 0;

        try {
            // Appel système pour récupérer les statuts
            // On utilise un userId fictif pour les appels internes
            List<StatusDTO> statuses = projectServiceClient.getProjectStatuses(projectId, 0L, "ADMIN");

            for (StatusDTO status : statuses) {
                Long count = taskRepository.countByProjectIdAndStatusId(projectId, status.getId());

                // Mapper par nom pour compatibilité avec l'ancien système
                if ("To Do".equalsIgnoreCase(status.getName()) || "TODO".equalsIgnoreCase(status.getName())) {
                    todoTasks = count.intValue();
                } else if ("In Progress".equalsIgnoreCase(status.getName()) || "IN_PROGRESS".equalsIgnoreCase(status.getName())) {
                    inProgressTasks = count.intValue();
                } else if ("Done".equalsIgnoreCase(status.getName())) {
                    doneTasks = count.intValue();
                }
            }
        } catch (Exception e) {
            log.error("Failed to fetch statuses for stats, returning totals only", e);
        }

        return TaskStatsResponse.builder()
                .projectId(projectId)
                .totalTasks(totalTasks.intValue())
                .todoTasks(todoTasks)
                .inProgressTasks(inProgressTasks)
                .doneTasks(doneTasks)
                .build();
    }

    // ========== MÉTHODES PRIVÉES ==========

    /**
     * Valider qu'un statut existe et appartient au projet
     */
    private StatusDTO validateStatus(Long projectId, Long statusId, Long userId, String role) {
        try {
            StatusDTO status = projectServiceClient.getStatusById(projectId, statusId, userId, role);

            if (status == null || !status.getProjectId().equals(projectId)) {
                throw new BadRequestException("Invalid status for this project");
            }

            return status;
        } catch (FeignException.NotFound e) {
            throw new ResourceNotFoundException("Status not found");
        } catch (FeignException.Forbidden e) {
            throw new BadRequestException("No access to this status");
        } catch (Exception e) {
            log.error("Failed to validate status {} for project {}", statusId, projectId, e);
            throw new BadRequestException("Invalid status or project");
        }
    }

    /**
     * Obtenir le premier statut d'un projet (par défaut)
     */
    private StatusDTO getFirstProjectStatus(Long projectId, Long userId, String role) {
        try {
            List<StatusDTO> statuses = projectServiceClient.getProjectStatuses(projectId, userId, role);

            if (statuses.isEmpty()) {
                throw new BadRequestException("No statuses found for this project");
            }

            // Retourner le premier statut (normalement "To Do")
            return statuses.get(0);
        } catch (Exception e) {
            log.error("Failed to fetch statuses for project {}", projectId, e);
            throw new BadRequestException("Failed to get project statuses");
        }
    }

    /**
     * Récupérer les détails d'un statut
     */
    private StatusDTO fetchStatusDetails(Long projectId, Long statusId, Long userId, String role) {
        try {
            return projectServiceClient.getStatusById(projectId, statusId, userId, role);
        } catch (Exception e) {
            log.error("Failed to fetch status details", e);
            // Retourner un statut minimal si l'appel échoue
            return StatusDTO.builder()
                    .id(statusId)
                    .name("Unknown")
                    .color("#999999")
                    .build();
        }
    }

    /**
     * Valider l'accès au projet
     */
    private void verifyProjectAccess(Long projectId, Long userId, String role) {
        try {
            projectServiceClient.getProjectById(projectId, userId, role);
        } catch (FeignException.NotFound e) {
            throw new ResourceNotFoundException("Project not found");
        } catch (FeignException.Forbidden e) {
            throw new BadRequestException("No access to this project");
        } catch (Exception e) {
            log.error("Failed to verify project access for projectId: {}", projectId, e);
            throw new BadRequestException("Failed to verify project access");
        }
    }

    /**
     * Enrichir les tâches avec les détails des statuts (batch)
     */
    private Page<TaskResponse> enrichTasksWithStatuses(Page<Task> tasks, Long projectId, Long userId, String role) {
        try {
            // Récupérer tous les statuts du projet en une seule fois
            List<StatusDTO> statuses = projectServiceClient.getProjectStatuses(projectId, userId, role);

            // Créer un map pour lookup rapide
            Map<Long, StatusDTO> statusMap = statuses.stream()
                    .collect(Collectors.toMap(StatusDTO::getId, s -> s));

            // Enrichir les tâches
            return tasks.map(task -> {
                StatusDTO status = statusMap.get(task.getStatusId());
                if (status != null) {
                    return mapToTaskResponse(task, status);
                } else {
                    return mapToTaskResponseWithoutStatus(task);
                }
            });
        } catch (Exception e) {
            log.error("Failed to enrich tasks with statuses", e);
            // Fallback: retourner les tâches sans enrichissement
            return tasks.map(this::mapToTaskResponseWithoutStatus);
        }
    }

    /**
     * Mapper Task vers TaskResponse avec statut enrichi
     */
    private TaskResponse mapToTaskResponse(Task task, StatusDTO status) {
        return TaskResponse.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(status)
                .priority(task.getPriority())
                .dueDate(task.getDueDate())
                .projectId(task.getProjectId())
                .assignedUser(task.getAssignedTo())
                .position(task.getPosition())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }

    /**
     * Mapper Task vers TaskResponse sans détails de statut (fallback)
     */
    private TaskResponse mapToTaskResponseWithoutStatus(Task task) {
        return TaskResponse.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(StatusDTO.builder()
                        .id(task.getStatusId())
                        .name("Unknown")
                        .color("#999999")
                        .build())
                .priority(task.getPriority())
                .dueDate(task.getDueDate())
                .projectId(task.getProjectId())
                .assignedUser(task.getAssignedTo())
                .position(task.getPosition())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }
}
