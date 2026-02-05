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

    /**
     * Create a new project
     * The authenticated user becomes the project owner
     */
    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(
            @Valid @RequestBody ProjectRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        ProjectResponse response = projectService.createProject(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get all projects
     * - ADMIN sees all projects
     * - USER sees only their own projects (as owner or member)
     */
    @GetMapping
    public ResponseEntity<Page<ProjectResponse>> getAllProjects(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role,
            Pageable pageable) {
        Page<ProjectResponse> projects = projectService.getAllProjects(userId, role, pageable);
        return ResponseEntity.ok(projects);
    }

    /**
     * Get a specific project by ID
     * User must be owner, member, or admin
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponse> getProjectById(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role) {
        ProjectResponse project = projectService.getProjectById(id, userId, role);
        return ResponseEntity.ok(project);
    }

    /**
     * Update a project
     * Only the project owner or admin can update
     */
    @PutMapping("/{id}")
    public ResponseEntity<ProjectResponse> updateProject(
            @PathVariable Long id,
            @Valid @RequestBody ProjectRequest request,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role) {
        ProjectResponse project = projectService.updateProject(id, request, userId, role);
        return ResponseEntity.ok(project);
    }

    /**
     * Delete a project
     * Only the project owner or admin can delete
     * This will cascade delete associated members and invitations
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role) {
        projectService.deleteProject(id, userId, role);
        return ResponseEntity.noContent().build();
    }

    /**
     * Add a member to a project
     * Only the project owner or admin can add members
     */
    @PostMapping("/{id}/members")
    public ResponseEntity<MemberResponse> addMember(
            @PathVariable Long id,
            @Valid @RequestBody AddMemberRequest request,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role) {
        MemberResponse member = projectService.addMember(id, request, userId, role);
        return ResponseEntity.status(HttpStatus.CREATED).body(member);
    }

    /**
     * Get all members of a project
     * User must be owner, member, or admin to view members
     */
    @GetMapping("/{id}/members")
    public ResponseEntity<List<MemberResponse>> getProjectMembers(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role) {
        List<MemberResponse> members = projectService.getProjectMembers(id, userId, role);
        return ResponseEntity.ok(members);
    }

    /**
     * Remove a member from a project
     * Only the project owner or admin can remove members
     * The project owner cannot be removed
     */
    @DeleteMapping("/{id}/members/{userId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable Long id,
            @PathVariable Long userId,
            @RequestHeader("X-User-Id") Long requesterId,
            @RequestHeader("X-User-Role") String role) {
        projectService.removeMember(id, userId, requesterId, role);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get project statistics
     * Shows task counts and member count
     * User must be owner, member, or admin
     */
    @GetMapping("/{id}/stats")
    public ResponseEntity<ProjectStatsResponse> getProjectStats(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role) {
        ProjectStatsResponse stats = projectService.getProjectStats(id, userId, role);
        return ResponseEntity.ok(stats);
    }

    /**
     * Invite a member to a project
     * Only the project owner or admin can send invitations
     *
     * - If the user exists: adds them directly
     * - If the user doesn't exist: creates a pending invitation
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
                role
        );

        return ResponseEntity.ok(response);
    }
}