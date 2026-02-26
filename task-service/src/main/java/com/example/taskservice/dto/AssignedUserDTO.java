package com.example.taskservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignedUserDTO {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String avatarUrl;
}
