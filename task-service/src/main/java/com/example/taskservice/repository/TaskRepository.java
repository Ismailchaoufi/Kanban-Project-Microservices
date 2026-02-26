package com.example.taskservice.repository;

import com.example.taskservice.entity.Priority;
import com.example.taskservice.entity.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Task entity
 * All queries use statusId (Long) instead of status entity
 */
@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    // ===== Find by Project =====

    Page<Task> findByProjectId(Long projectId, Pageable pageable);

    List<Task> findByProjectIdOrderByPositionAsc(Long projectId);

    // ===== Find by Status ID =====

    Page<Task> findByStatusId(Long statusId, Pageable pageable);

    List<Task> findByStatusIdOrderByPositionAsc(Long statusId);

    // ===== Find by Project and Status ID =====

    Page<Task> findByProjectIdAndStatusId(Long projectId, Long statusId, Pageable pageable);

    List<Task> findByProjectIdAndStatusIdOrderByPositionAsc(Long projectId, Long statusId);

    // ===== Find by Assigned User =====

    Page<Task> findByAssignedTo(Long assignedTo, Pageable pageable);

    Page<Task> findByProjectIdAndAssignedTo(Long projectId, Long assignedTo, Pageable pageable);

    // ===== Find by Priority =====

    Page<Task> findByPriority(Priority priority, Pageable pageable);

    Page<Task> findByProjectIdAndPriority(Long projectId, Priority priority, Pageable pageable);

    // ===== Count Methods =====

    Long countByProjectId(Long projectId);

    Long countByProjectIdAndStatusId(Long projectId, Long statusId);

    Long countByStatusId(Long statusId);

    Long countByAssignedTo(Long assignedTo);

    // ===== Delete Methods =====

    /**
     * Delete all tasks for a project
     * Use when project is deleted
     */
    void deleteByProjectId(Long projectId);

    /**
     * Move all tasks from one status to another
     * Use when a status is deleted in Project Service
     */
    @Query("UPDATE Task t SET t.statusId = :newStatusId WHERE t.statusId = :oldStatusId")
    void moveTasksToStatus(@Param("oldStatusId") Long oldStatusId, @Param("newStatusId") Long newStatusId);

    // ===== Position Management =====

    // ===== Search =====

    @Query("SELECT t FROM Task t WHERE t.projectId = :projectId AND " +
            "(LOWER(t.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(t.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Task> searchTasksByProject(
            @Param("projectId") Long projectId,
            @Param("search") String search,
            Pageable pageable
    );

    @Query(value = "SELECT COALESCE(MAX(position), -1) FROM tasks WHERE status_id = :statusId",
            nativeQuery = true)
    Integer findMaxPositionByStatusId(@Param("statusId") Long statusId);
}