package com.examlple.projectservice.repository;

import com.examlple.projectservice.entity.Project;
import com.examlple.projectservice.entity.ProjectStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    // Find projects by owner
    Page<Project> findByOwnerId(Long ownerId, Pageable pageable);

    // Find projects where user is a member
    @Query("SELECT DISTINCT p FROM Project p JOIN p.members m WHERE m.userId = :userId")
    Page<Project> findByMemberId(@Param("userId") Long userId, Pageable pageable);

    // Find all projects for user (owner or member)
    @Query("SELECT DISTINCT p FROM Project p LEFT JOIN p.members m " +
            "WHERE p.ownerId = :userId OR m.userId = :userId")
    Page<Project> findByOwnerIdOrMemberId(@Param("userId") Long userId, Pageable pageable);

    // Find by status
    Page<Project> findByStatus(ProjectStatus status, Pageable pageable);

    // Count projects by owner
    Long countByOwnerId(Long ownerId);

    // Count all projects for user
    @Query("SELECT COUNT(DISTINCT p) FROM Project p LEFT JOIN p.members m " +
            "WHERE p.ownerId = :userId OR m.userId = :userId")
    Long countByOwnerIdOrMemberId(@Param("userId") Long userId);
}
