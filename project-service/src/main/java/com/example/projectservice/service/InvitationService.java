package com.example.projectservice.service;

import com.example.projectservice.client.AuthServiceClient;
import com.example.projectservice.dto.UserDTO;
import com.example.projectservice.entity.Invitation;
import com.example.projectservice.entity.InvitationStatus;
import com.example.projectservice.entity.Project;
import com.example.projectservice.exception.BadRequestException;
import com.example.projectservice.exception.ForbiddenException;
import com.example.projectservice.exception.ResourceNotFoundException;
import com.example.projectservice.repository.InvitationRepository;
import com.example.projectservice.repository.ProjectRepository;
import com.example.projectservice.dto.InvitationInfoResponse;
import com.example.projectservice.dto.InvitationRequest;
import com.example.projectservice.dto.InvitationResponse;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    /**
     * Invite a member to a project
     * - Only OWNER or ADMIN can invite members
     * - If user exists, adds them directly
     * - If user doesn't exist, creates a pending invitation
     *
     */
    @Transactional
    public InvitationResponse inviteMember(Long projectId, InvitationRequest request, Long inviterId, String inviterRole) {

        // Verify permissions BEFORE any other operations
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        boolean isAdmin = "ADMIN".equals(inviterRole);
        boolean isOwner = project.getOwnerId().equals(inviterId);

        if (!isAdmin && !isOwner) {
            throw new ForbiddenException("Only the project owner or admin can invite members");
        }

        // Validate email format
        String email = request.getEmail().toLowerCase().trim();
        if (!isValidEmail(email)) {
            throw new BadRequestException("Invalid email format");
        }

        // Use database constraint to prevent race condition
        // The database should have a UNIQUE constraint on (projectId, email, status='PENDING')
        if (invitationRepository.existsByProjectIdAndEmailAndStatus(projectId, email, InvitationStatus.PENDING)) {
            throw new BadRequestException("Invitation already sent to this email");
        }

        // Verify if user exists
        try {
            UserDTO user = authServiceClient.getUserByEmail(email);

            // User exists → add directly
            projectService.addMemberFromInvitation(projectId, user.getId());
            // Send email asynchronously (already marked with @Async)
            emailService.sendDirectAddEmail(email, projectId, project.getTitle());

            log.info("User {} added directly to project {}", user.getId(), projectId);
            return InvitationResponse.builder()
                    .email(email)
                    .message("User added successfully to the project")
                    .userExists(true)
                    .build();

        } catch (FeignException.NotFound e) {
            // User doesn't exist → create pending invitation
            Invitation invitation = Invitation.builder()
                    .projectId(projectId)
                    .email(email)
                    .invitedBy(inviterId)
                    .token(UUID.randomUUID().toString())
                    .status(InvitationStatus.PENDING)
                    .expiresAt(LocalDateTime.now().plusDays(7))
                    .build();

            invitationRepository.save(invitation);

            // Send email asynchronously
            emailService.sendInvitationEmail(invitation, project.getTitle());

            log.info("Invitation created for {} to project {}", email, projectId);
            return InvitationResponse.builder()
                    .email(email)
                    .message("Invitation sent successfully")
                    .userExists(false)
                    .build();
        }
    }

    /**
     * Accept a pending invitation
     * - Verifies the invitation token
     * - Checks if the invitation is still valid (not expired, not already accepted)
     * - Verifies email matches the authenticated user
     * - Adds user as a project member
     */
    @Transactional
    public void acceptInvitation(String token, Long userId) {
        Invitation invitation = invitationRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid invitation token"));

        // Check invitation status
        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new BadRequestException("This invitation is no longer valid (status: " + invitation.getStatus() + ")");
        }

        // Check expiration
        if (invitation.getExpiresAt().isBefore(LocalDateTime.now())) {
            invitation.setStatus(InvitationStatus.EXPIRED);
            invitationRepository.save(invitation);
            log.warn("Expired invitation used for token: {}", token);
            throw new BadRequestException("This invitation has expired");
        }

        // Verify user exists and get their details
        UserDTO user = authServiceClient.getUserByIdInternal(userId);

        // Verify email matches (case-insensitive)
        if (!user.getEmail().equalsIgnoreCase(invitation.getEmail())) {
            log.warn("Email mismatch: invitation for {}, user has {}", invitation.getEmail(), user.getEmail());
            throw new BadRequestException("This invitation was sent to a different email address");
        }

        // Add user as member
        projectService.addMemberFromInvitation(invitation.getProjectId(), userId);

        // Mark invitation as accepted
        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitationRepository.save(invitation);

        log.info("User {} accepted invitation {} for project {}", userId, token, invitation.getProjectId());
    }

    /**
     * Get invitation information (public - no authentication required)
     * - Returns project details
     * - Shows inviter information
     * - Indicates if invitation is still valid
     */
    @Transactional(readOnly = true)
    public InvitationInfoResponse getInvitationInfo(String token) {
        Invitation invitation = invitationRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid invitation token"));

        Project project = projectRepository.findById(invitation.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        // Check if invitation is in a valid state
        if (invitation.getStatus() != InvitationStatus.PENDING) {
            log.warn("Requested info for non-pending invitation: {}", token);
        }

        UserDTO inviter = authServiceClient.getUserById(invitation.getInvitedBy(), 1L, "ADMIN");

        boolean expired = invitation.getExpiresAt().isBefore(LocalDateTime.now());
        boolean valid = invitation.getStatus() == InvitationStatus.PENDING && !expired;

        return InvitationInfoResponse.builder()
                .email(invitation.getEmail())
                .projectName(project.getTitle())
                .invitedBy(inviter.getFirstName() + " " + inviter.getLastName())
                .expired(expired)
                .valid(valid)
                .build();
    }

    /**
     * Validate email format
     * Simple validation - use a proper validator in production
     */
    private boolean isValidEmail(String email) {
        // Basic regex pattern for email validation
        // In production, consider using @Email annotation with proper validator
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return email.matches(emailRegex);
    }
}