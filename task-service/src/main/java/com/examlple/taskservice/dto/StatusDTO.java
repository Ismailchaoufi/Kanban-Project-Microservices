package com.examlple.taskservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour le statut d'une tâche dans les réponses
 * Contient seulement les informations essentielles du statut
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatusDTO {
    private Long id;
    private String name;
    private String color;
    private Long projectId;
    private Integer position;
    private Boolean isDefault;
}