package com.examlple.taskservice.service;

import com.examlple.taskservice.entity.TaskStatusEntity;
import com.examlple.taskservice.repository.TaskStatusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service to initialize default statuses for projects
 * This ensures every project has the 3 default statuses: To Do, In Progress, Done
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectStatusInitializer {

    private final TaskStatusRepository taskStatusRepository;

    /**
     * Initialize default statuses for a new project
     * Creates 3 statuses: To Do, In Progress, Done
     *
     * Call this method when a new project is created
     */
    @Transactional
    public void initializeDefaultStatuses(Long projectId) {
        // Check if project already has statuses
        long existingCount = taskStatusRepository.countByProjectId(projectId);

        if (existingCount > 0) {
            log.info("Project {} already has {} statuses, skipping initialization",
                    projectId, existingCount);
            return;
        }

        // Create default statuses
        createDefaultStatus(projectId, "To Do", "#ff9800", 0, true);
        createDefaultStatus(projectId, "In Progress", "#2196f3", 1, true);
        createDefaultStatus(projectId, "Done", "#4caf50", 2, true);

        log.info("Initialized default statuses for project {}", projectId);
    }

    /**
     * Create a single default status
     */
    private void createDefaultStatus(Long projectId, String name, String color,
                                     int position, boolean isDefault) {
        TaskStatusEntity status = new TaskStatusEntity();
        status.setProjectId(projectId);
        status.setName(name);
        status.setColor(color);
        status.setPosition(position);
        status.setIsDefault(isDefault);

        taskStatusRepository.save(status);
        log.debug("Created default status '{}' for project {}", name, projectId);
    }

    /**
     * Initialize default statuses for all projects that don't have any
     * Useful for migrating existing projects
     */
    @Transactional
    public void initializeDefaultStatusesForAllProjects() {
        // This would require ProjectRepository to get all projects
        // For now, this is a placeholder
        log.info("Batch initialization of default statuses - implement based on your project structure");
    }
}