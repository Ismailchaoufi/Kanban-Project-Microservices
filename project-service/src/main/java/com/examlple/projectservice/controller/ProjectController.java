package com.examlple.projectservice.controller;

import com.examlple.projectservice.dto.*;
import com.examlple.projectservice.service.InvitationService;
import com.examlple.projectservice.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final InvitationService invitationService;

    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(
            @Valid @RequestBody ProjectRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        ProjectResponse response = projectService.createProject(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<ProjectResponse>> getAllProjects(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role,
            Pageable pageable) {
        Page<ProjectResponse> projects = projectService.getAllProjects(userId, role, pageable);
        return ResponseEntity.ok(projects);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponse> getProjectById(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role) {
        ProjectResponse project = projectService.getProjectById(id, userId, role);
        return ResponseEntity.ok(project);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProjectResponse> updateProject(
            @PathVariable Long id,
            @Valid @RequestBody ProjectRequest request,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role) {
        ProjectResponse project = projectService.updateProject(id, request, userId, role);
        return ResponseEntity.ok(project);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role) {
        projectService.deleteProject(id, userId, role);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<MemberResponse> addMember(
            @PathVariable Long id,
            @Valid @RequestBody AddMemberRequest request,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role) {
        MemberResponse member = projectService.addMember(id, request, userId, role);
        return ResponseEntity.status(HttpStatus.CREATED).body(member);
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<List<MemberResponse>> getProjectMembers(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role) {
        List<MemberResponse> members = projectService.getProjectMembers(id, userId, role);
        return ResponseEntity.ok(members);
    }

    @DeleteMapping("/{id}/members/{userId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable Long id,
            @PathVariable Long userId,
            @RequestHeader("X-User-Id") Long requesterId,
            @RequestHeader("X-User-Role") String role) {
        projectService.removeMember(id, userId, requesterId, role);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/stats")
    public ResponseEntity<ProjectStatsResponse> getProjectStats(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role) {
        ProjectStatsResponse stats = projectService.getProjectStats(id, userId, role);
        return ResponseEntity.ok(stats);
    }

    /**
     * Inviter un membre (OWNER ou ADMIN uniquement)
     */
    @PostMapping("/{projectId}/invite")
    public ResponseEntity<InvitationResponse> inviteMember(
            @PathVariable Long projectId,
            @Valid @RequestBody InvitationRequest request,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role
    ) {
        InvitationResponse response = invitationService.inviteMember(
                projectId,
                request,
                userId,
                role  // 🔑 Passer le rôle pour vérification
        );

        return ResponseEntity.ok(response);
    }

//    /**
//     * Accepter une invitation (accessible à tous les utilisateurs authentifiés)
//     */
//    @PostMapping("/invitations/accept")
//    public ResponseEntity<String> acceptInvitation(
//            @RequestParam String token,
//            @RequestHeader("X-User-Id") Long userId
//    ) {
//        invitationService.acceptInvitation(token, userId);
//        return ResponseEntity.ok("Invitation accepted successfully");
//    }
//
//    /**
//     * Vérifier un token d'invitation (PUBLIC - pas besoin d'authentification)
//     */
//    @GetMapping("/invitations/verify")
//    public ResponseEntity<InvitationInfoResponse> verifyInvitation(@RequestParam String token) {
//        InvitationInfoResponse info = invitationService.getInvitationInfo(token);
//        return ResponseEntity.ok(info);
//    }

//    /**
//     * Annuler une invitation (OWNER ou ADMIN uniquement)
//     */
//    @DeleteMapping("/invitations/{invitationId}")
//    public ResponseEntity<Void> cancelInvitation(
//            @PathVariable Long invitationId,
//            @RequestHeader("X-User-Id") Long userId,
//            @RequestHeader("X-User-Role") String role
//    ) {
//        invitationService.cancelInvitation(invitationId, userId, role);
//        return ResponseEntity.noContent().build();
//    }
}
