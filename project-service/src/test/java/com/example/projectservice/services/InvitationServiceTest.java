package com.example.projectservice.services;

import com.example.projectservice.client.AuthServiceClient;
import com.example.projectservice.dto.InvitationRequest;
import com.example.projectservice.dto.InvitationResponse;
import com.example.projectservice.dto.MemberResponse;
import com.example.projectservice.dto.UserDTO;
import com.example.projectservice.entity.Invitation;
import com.example.projectservice.entity.InvitationStatus;
import com.example.projectservice.entity.Project;
import com.example.projectservice.exception.BadRequestException;
import com.example.projectservice.exception.ForbiddenException;
import com.example.projectservice.repository.InvitationRepository;
import com.example.projectservice.repository.ProjectRepository;
import com.example.projectservice.service.EmailService;
import com.example.projectservice.service.InvitationService;
import com.example.projectservice.service.ProjectService;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class InvitationServiceTest {
    @Mock
    private InvitationRepository invitationRepository;

    @Mock
    private AuthServiceClient authServiceClient;

    @Mock
    private ProjectService projectService;

    @Mock
    private EmailService emailService;

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private InvitationService invitationService;

    private Project sampleProject;

    @BeforeEach
    void setUp() {
        sampleProject = new Project();
        sampleProject.setId(1L);
        sampleProject.setTitle("Projet Alpha");
        sampleProject.setOwnerId(10L);
    }

    // inviteMember
    @Test
    void inviteMember_ajoute_directement_si_utilisateur_existe() {
        InvitationRequest request = new InvitationRequest();
        request.setEmail("bob@example.com");

        UserDTO existingUser = new UserDTO();
        existingUser.setId(20L);
        existingUser.setEmail("bob@example.com");

        MemberResponse memberResponse = MemberResponse.builder()
                .userId(20L)
                .firstName("Bob")
                .build();

        when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject));
        when(invitationRepository.existsByProjectIdAndEmailAndStatus(1L, "bob@example.com", InvitationStatus.PENDING))
                .thenReturn(false);
        when(authServiceClient.getUserByEmail("bob@example.com")).thenReturn(existingUser);
        when(projectService.addMemberFromInvitation(1L, 20L)).thenReturn(memberResponse);

        InvitationResponse response = invitationService.inviteMember(1L, request, 10L, "USER");

        assertThat(response.isUserExists()).isTrue();
        assertThat(response.getMessage()).contains("added successfully");
        verify(projectService).addMemberFromInvitation(1L, 20L);
        verify(invitationRepository, never()).save(any());
    }

    @Test
    void inviteMember_cree_invitation_si_utilisateur_nexiste_pas() {
        InvitationRequest request = new InvitationRequest();
        request.setEmail("nouveau@example.com");

        when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject));
        when(invitationRepository.existsByProjectIdAndEmailAndStatus(1L, "nouveau@example.com", InvitationStatus.PENDING))
                .thenReturn(false);
        when(authServiceClient.getUserByEmail("nouveau@example.com"))
                .thenThrow(FeignException.NotFound.class);

        InvitationResponse response = invitationService.inviteMember(1L, request, 10L, "USER");

        assertThat(response.isUserExists()).isFalse();
        assertThat(response.getMessage()).contains("Invitation sent");
        verify(invitationRepository).save(any(Invitation.class));
        verify(projectService, never()).addMemberFromInvitation(anyLong(), anyLong());
    }

    @Test
    void inviteMember_refuse_si_pas_owner_ni_admin() {
        InvitationRequest request = new InvitationRequest();
        request.setEmail("quelquun@example.com");

        when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject));

        // userId=99 n'est pas owner (10L)
        assertThatThrownBy(() -> invitationService.inviteMember(1L, request, 99L, "USER"))
                .isInstanceOf(ForbiddenException.class);

        verify(invitationRepository, never()).save(any());
    }

    @Test
    void inviteMember_refuse_si_email_invalide() {
        InvitationRequest request = new InvitationRequest();
        request.setEmail("pas-un-email");

        when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject));

        assertThatThrownBy(() -> invitationService.inviteMember(1L, request, 10L, "USER"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid email");
    }

    @Test
    void inviteMember_refuse_si_invitation_deja_envoyee() {
        InvitationRequest request = new InvitationRequest();
        request.setEmail("bob@example.com");

        when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject));
        when(invitationRepository.existsByProjectIdAndEmailAndStatus(1L, "bob@example.com", InvitationStatus.PENDING))
                .thenReturn(true);

        assertThatThrownBy(() -> invitationService.inviteMember(1L, request, 10L, "USER"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already sent");
    }

    // acceptInvitation
    @Test
    void acceptInvitation_fonctionne_avec_un_token_valide() {
        Invitation invitation = Invitation.builder()
                .projectId(1L)
                .email("bob@example.com")
                .token("valid-token")
                .status(InvitationStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusDays(3))
                .build();

        UserDTO user = new UserDTO();
        user.setId(20L);
        user.setEmail("bob@example.com");

        when(invitationRepository.findByToken("valid-token")).thenReturn(Optional.of(invitation));
        when(authServiceClient.getUserByIdInternal(20L)).thenReturn(user);
        when(projectService.addMemberFromInvitation(1L, 20L)).thenReturn(mock(MemberResponse.class));

        invitationService.acceptInvitation("valid-token", 20L);

        assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.ACCEPTED);
        verify(invitationRepository).save(invitation);
    }


}
