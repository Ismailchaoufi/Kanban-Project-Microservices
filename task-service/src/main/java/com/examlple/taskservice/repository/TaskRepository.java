package com.examlple.taskservice.repository;

import com.examlple.taskservice.entity.Priority;
import com.examlple.taskservice.entity.Task;
import com.examlple.taskservice.entity.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    // Find tasks by project
    Page<Task> findByProjectId(Long projectId, Pageable pageable);
    List<Task> findByProjectId(Long projectId);

    // Find tasks by assigned user
    Page<Task> findByAssignedTo(Long assignedTo, Pageable pageable);

    // Find tasks by status
    Page<Task> findByStatus(TaskStatus status, Pageable pageable);

    // Find tasks by priority
    Page<Task> findByPriority(Priority priority, Pageable pageable);

    // Find tasks by project and status
    Page<Task> findByProjectIdAndStatus(Long projectId, TaskStatus status, Pageable pageable);

    // Find tasks by project and assigned user
    Page<Task> findByProjectIdAndAssignedTo(Long projectId, Long assignedTo, Pageable pageable);

    // Count tasks by project
    Long countByProjectId(Long projectId);

    // Count tasks by project and status
    Long countByProjectIdAndStatus(Long projectId, TaskStatus status);

    // Count tasks by assigned user
    Long countByAssignedTo(Long assignedTo);

    // Delete all tasks by project
    void deleteByProjectId(Long projectId);

    // Custom query for search
    @Query("SELECT t FROM Task t WHERE t.projectId = :projectId AND " +
            "(LOWER(t.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(t.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Task> searchTasksByProject(@Param("projectId") Long projectId,
                                    @Param("search") String search,
                                    Pageable pageable);
}
