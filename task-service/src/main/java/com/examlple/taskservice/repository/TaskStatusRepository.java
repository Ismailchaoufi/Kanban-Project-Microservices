package com.examlple.taskservice.repository;

import com.examlple.taskservice.entity.TaskStatusEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskStatusRepository extends JpaRepository<TaskStatusEntity, Long> {

    // Find all statuses for a project, ordered by position
    List<TaskStatusEntity> findByProjectIdOrderByPositionAsc(Long projectId);

    // Find status by project and name
    Optional<TaskStatusEntity> findByProjectIdAndName(Long projectId, String name);

    // Check if status name exists in project
    boolean existsByProjectIdAndName(Long projectId, String name);

    // Count statuses in a project
    long countByProjectId(Long projectId);

    // Find status by ID and project (for security)
    Optional<TaskStatusEntity> findByIdAndProjectId(Long id, Long projectId);

    // Delete all statuses for a project
    void deleteByProjectId(Long projectId);

    // Get max position for a project
    @Query("SELECT COALESCE(MAX(s.position), -1) FROM TaskStatusEntity s WHERE s.projectId = :projectId")
    Integer findMaxPositionByProjectId(@Param("projectId") Long projectId);
}

