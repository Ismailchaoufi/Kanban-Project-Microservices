package com.examlple.projectservice.service;

import com.examlple.projectservice.client.AuthServiceClient;
import com.examlple.projectservice.dto.AddMemberRequest;
import com.examlple.projectservice.dto.InvitationRequest;
import com.examlple.projectservice.dto.InvitationResponse;
import com.examlple.projectservice.dto.UserDTO;
import com.examlple.projectservice.entity.Invitation;
import com.examlple.projectservice.entity.InvitationStatus;
import com.examlple.projectservice.entity.Project;
import com.examlple.projectservice.exception.BadRequestException;
import com.examlple.projectservice.exception.ForbiddenException;
import com.examlple.projectservice.exception.ResourceNotFoundException;
import com.examlple.projectservice.repository.InvitationRepository;
import com.examlple.projectservice.repository.ProjectRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvitationService {

    private final InvitationRepository invitationRepository;
    private final AuthServiceClient authServiceClient;
    private final ProjectService projectService;
    private final EmailService emailService;
    private final ProjectRepository projectRepository;

    @Transactional
    public InvitationResponse inviteMember(Long projectId, InvitationRequest request, Long inviterId, String inviterRole) {

        // Vérifier les permissions AVANT tout
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        boolean isAdmin = "ADMIN".equals(inviterRole);
        boolean isOwner = project.getOwnerId().equals(inviterId);

        if (!isAdmin && !isOwner) {
            throw new ForbiddenException("Only the project owner or admin can invite members");
        }

        String email = request.getEmail().toLowerCase().trim();

        // Vérifier si invitation existe déjà
        if (invitationRepository.existsByProjectIdAndEmailAndStatus(projectId, email, InvitationStatus.PENDING)) {
            throw new BadRequestException("Invitation already sent to this email");
        }

        // Vérifier si l'utilisateur existe
        try {
            UserDTO user = authServiceClient.getUserByEmail(email);

            // Utilisateur existe → ajouter directement
            projectService.addMemberFromInvitation(projectId, user.getId());
            emailService.sendDirectAddEmail(email, projectId, project.getTitle());

            return InvitationResponse.builder()
                    .email(email)
                    .message("User added successfully to the project")
                    .userExists(true)
                    .build();

        } catch (FeignException.NotFound e) {
            // Utilisateur n'existe pas → créer invitation
            Invitation invitation = Invitation.builder()
                    .projectId(projectId)
                    .email(email)
                    .invitedBy(inviterId)
                    .token(UUID.randomUUID().toString())
                    .status(InvitationStatus.PENDING)
                    .expiresAt(LocalDateTime.now().plusDays(7))
                    .build();

            invitationRepository.save(invitation);
            emailService.sendInvitationEmail(invitation, project.getTitle());

            return InvitationResponse.builder()
                    .email(email)
                    .message("Invitation sent successfully")
                    .userExists(false)
                    .build();
        }
    }

    @Transactional
    public void acceptInvitation(String token, Long userId) {
        Invitation invitation = invitationRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid invitation token"));

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new BadRequestException("This invitation is no longer valid");
        }

        // Vérifier l'expiration
        if (invitation.getExpiresAt().isBefore(LocalDateTime.now())) {
            invitation.setStatus(InvitationStatus.EXPIRED);
            invitationRepository.save(invitation);
            throw new BadRequestException("This invitation has expired");
        }

        // Ajouter le membre
        projectService.addMemberFromInvitation(invitation.getProjectId(), userId);

        // Mettre à jour l'invitation
        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitationRepository.save(invitation);

        log.info("User {} accepted invitation for project {}", userId, invitation.getProjectId());
    }
}