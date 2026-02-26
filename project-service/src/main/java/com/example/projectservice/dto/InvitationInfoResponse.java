package com.example.projectservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvitationInfoResponse {
    private String email;
    private String projectName;
    private String invitedBy;
    private boolean expired;
    private boolean valid;
}
