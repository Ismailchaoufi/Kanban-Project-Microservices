package com.examlple.projectservice.service;

import com.examlple.projectservice.client.AuthServiceClient;
import com.examlple.projectservice.dto.AddMemberRequest;
import com.examlple.projectservice.dto.InvitationRequest;
import com.examlple.projectservice.dto.InvitationResponse;
import com.examlple.projectservice.dto.UserDTO;
import com.examlple.projectservice.entity.Invitation;
import com.examlple.projectservice.entity.InvitationStatus;
import com.examlple.projectservice.repository.InvitationRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InvitationService {

    private final InvitationRepository invitationRepository;
    private final AuthServiceClient authServiceClient;
    private final ProjectService projectService;
    private final EmailService emailService;

    @Transactional
    public InvitationResponse inviteMember(Long projectId, InvitationRequest request, Long inviterId) {
        String email = request.getEmail().toLowerCase().trim();

        // Vérifier si invitation existe déjà
        if (invitationRepository.existsByProjectIdAndEmailAndStatus(projectId, email, InvitationStatus.PENDING)) {
            throw new RuntimeException("Invitation already sent");
        }

        // Vérifier si l'utilisateur existe dans Auth Service
        try {
            UserDTO user = authServiceClient.getUserByEmail(email);

            // Utilisateur existe → ajouter directement
            projectService.addMember(projectId, user.getId());
            emailService.sendDirectAddEmail(email, projectId);

            return InvitationResponse.builder()
                    .email(email)
                    .message("User added successfully")
                    .userExists(true)
                    .build();

        } catch (FeignException.NotFound e) {
            // Utilisateur n'existe pas → créer invitation
            Invitation invitation = Invitation.builder()
                    .projectId(projectId)
                    .email(email)
                    .invitedBy(inviterId)
                    .token(UUID.randomUUID().toString())
                    .build();

            invitationRepository.save(invitation);
            emailService.sendInvitationEmail(invitation);

            return InvitationResponse.builder()
                    .email(email)
                    .message("Invitation sent")
                    .userExists(false)
                    .build();
        }
    }

    @Transactional
    public void acceptInvitation(String token, Long userId) {
        Invitation invitation = invitationRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid token"));

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new RuntimeException("Invitation not valid");
        }

        // Ajouter le membre
        projectService.addMember(invitation.getProjectId(), userId);

        // Mettre à jour l'invitation
        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitationRepository.save(invitation);
    }
}
