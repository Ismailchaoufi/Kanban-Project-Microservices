package com.examlple.taskservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "task_statuses", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"projectId", "name"}),
        @UniqueConstraint(columnNames = {"projectId", "position"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskStatusEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false)
    private String color;

    @Column(nullable = false)
    private Long projectId;

    @Column(nullable = false)
    private Integer position; // For ordering columns

    @Column(nullable = false)
    private Boolean isDefault = false; // Mark default statuses (TODO, IN_PROGRESS, DONE)

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}