package com.example.projectservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing a task status (Kanban column)
 * Belongs to a project - like Trello lists belong to boards
 */
@Entity
@Table(name = "task_statuses", uniqueConstraints = {
        @UniqueConstraint(name = "uk_status_project_name", columnNames = {"project_id", "name"}),
        @UniqueConstraint(name = "uk_status_project_position", columnNames = {"project_id", "position"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskStatusEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, length = 7)
    private String color;

    @Column(nullable = false)
    private Integer position;

    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}