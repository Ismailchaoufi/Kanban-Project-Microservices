package com.examlple.projectservice.service;

import com.examlple.projectservice.client.AuthServiceClient;
import com.examlple.projectservice.client.TaskServiceClient;
import com.examlple.projectservice.dto.*;
import com.examlple.projectservice.entity.Project;
import com.examlple.projectservice.entity.ProjectMember;
import com.examlple.projectservice.entity.ProjectStatus;
import com.examlple.projectservice.entity.TaskStatsDTO;
import com.examlple.projectservice.exception.BadRequestException;
import com.examlple.projectservice.exception.ForbiddenException;
import com.examlple.projectservice.exception.ResourceNotFoundException;
import com.examlple.projectservice.repository.ProjectMemberRepository;
import com.examlple.projectservice.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository memberRepository;
    private final AuthServiceClient authServiceClient;
    private final TaskServiceClient taskServiceClient;



    @Transactional //cette methode est défini comme une transaction soit tout passé, soit tout annulé
    public ProjectResponse createProject(ProjectRequest request, Long ownerId) {
        Project project = new Project();
        project.setTitle(request.getTitle());
        project.setDescription(request.getDescription());
        project.setStatus(request.getStatus() != null ? request.getStatus() : ProjectStatus.IN_PROGRESS);
        project.setColor(request.getColor());
        project.setStartDate(request.getStartDate());
        project.setEndDate(request.getEndDate());
        project.setOwnerId(ownerId);

        Project savedProject = projectRepository.save(project);

        log.info("Project created successfully with ID: {}", savedProject.getId());
        return mapToProjectResponse(savedProject, ownerId, "USER");
    }

    @Transactional(readOnly = true)
    public Page<ProjectResponse> getAllProjects(Long userId, String role, Pageable pageable) {
        Page<Project> projects;

        if ("ADMIN".equals(role)) {
            // Admin can see all projects
            projects = projectRepository.findAll(pageable);
        } else {
            // User can only see projects where they are owner or member
            projects = projectRepository.findByOwnerIdOrMemberId(userId, pageable);
        }

        return projects.map(project -> mapToProjectResponse(project, userId, role));
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProjectById(Long projectId, Long userId, String role) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        // Check authorization
        if (!canAccessProject(project, userId, role)) {
            throw new ForbiddenException("You don't have permission to access this project");
        }

        return mapToProjectResponse(project, userId, role);
    }

    @Transactional
    public ProjectResponse updateProject(Long projectId, ProjectRequest request, Long userId, String role) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        // Only owner can update project
        if (!project.getOwnerId().equals(userId) && !"ADMIN".equals(role)) {
            throw new ForbiddenException("Only the project owner can update the project");
        }

        // Update fields
        if (request.getTitle() != null) {
            project.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            project.setDescription(request.getDescription());
        }
        if (request.getStatus() != null) {
            project.setStatus(request.getStatus());
        }
        if (request.getColor() != null) {
            project.setColor(request.getColor());
        }
        if (request.getStartDate() != null) {
            project.setStartDate(request.getStartDate());
        }
        if (request.getEndDate() != null) {
            project.setEndDate(request.getEndDate());
        }

        Project updatedProject = projectRepository.save(project);
        log.info("Project {} updated successfully", projectId);

        return mapToProjectResponse(updatedProject, userId, role);
    }

    @Transactional
    public void deleteProject(Long projectId, Long userId, String role) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        // Only owner can delete project
        if (!project.getOwnerId().equals(userId) && !"ADMIN".equals(role)) {
            throw new ForbiddenException("Only the project owner can delete the project");
        }

        projectRepository.delete(project);
        log.info("Project {} deleted successfully", projectId);
    }

    @Transactional
    public MemberResponse addMember(Long projectId, AddMemberRequest request, Long requesterId, String role) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        // Only owner can add members
        if (!project.getOwnerId().equals(requesterId) && !"ADMIN".equals(role)) {
            throw new ForbiddenException("Only the project owner can add members");
        }

        // Check if user is already a member
        if (memberRepository.existsByProjectIdAndUserId(projectId, request.getUserId())) {
            throw new BadRequestException("User is already a member of this project");
        }

        // Verify user exists via Auth Service
        UserDTO user;
        try {
            user = authServiceClient.getUserById(request.getUserId(), requesterId, role);
        } catch (Exception e) {
            throw new BadRequestException("User not found");
        }

        // Add member
        ProjectMember member = new ProjectMember();
        member.setProject(project);
        member.setUserId(request.getUserId());

        ProjectMember savedMember = memberRepository.save(member);
        log.info("User {} added to project {}", request.getUserId(), projectId);

        return mapToMemberResponse(savedMember, user);
    }

    @Transactional(readOnly = true)
    public List<MemberResponse> getProjectMembers(Long projectId, Long userId, String role) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        // Check authorization
        if (!canAccessProject(project, userId, role)) {
            throw new ForbiddenException("You don't have permission to access this project");
        }

        List<ProjectMember> members = memberRepository.findByProjectId(projectId);

        return members.stream()
                .map(member -> {
                    try {
                        UserDTO user = authServiceClient.getUserById(member.getUserId(), userId, role);
                        return mapToMemberResponse(member, user);
                    } catch (Exception e) {
                        log.error("Failed to fetch user details for userId: {}", member.getUserId());
                        return null;
                    }
                })
                .filter(memberResponse -> memberResponse != null)
                .collect(Collectors.toList());
    }

    @Transactional
    public void removeMember(Long projectId, Long userIdToRemove, Long requesterId, String role) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        // Only owner can remove members
        if (!project.getOwnerId().equals(requesterId) && !"ADMIN".equals(role)) {
            throw new ForbiddenException("Only the project owner can remove members");
        }

        // Owner cannot remove themselves
        if (project.getOwnerId().equals(userIdToRemove)) {
            throw new BadRequestException("Owner cannot be removed from the project");
        }

        ProjectMember member = memberRepository.findByProjectIdAndUserId(projectId, userIdToRemove)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found in this project"));

        memberRepository.delete(member);
        log.info("User {} removed from project {}", userIdToRemove, projectId);
    }

    @Transactional(readOnly = true)
    public ProjectStatsResponse getProjectStats(Long projectId, Long userId, String role) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        // Check authorization
        if (!canAccessProject(project, userId, role)) {
            throw new ForbiddenException("You don't have permission to access this project");
        }

        // Get task stats from Task Service
        TaskStatsDTO taskStats;
        try {
            taskStats = taskServiceClient.getTaskStatsByProject(projectId);
        } catch (Exception e) {
            log.error("Failed to fetch task stats for project {}", projectId);
            taskStats = TaskStatsDTO.builder()
                    .projectId(projectId)
                    .totalTasks(0)
                    .todoTasks(0)
                    .inProgressTasks(0)
                    .doneTasks(0)
                    .build();
        }

        Long memberCount = memberRepository.countByProjectId(projectId);

        return ProjectStatsResponse.builder()
                .projectId(project.getId())
                .projectTitle(project.getTitle())
                .totalTasks(taskStats.getTotalTasks())
                .todoTasks(taskStats.getTodoTasks())
                .inProgressTasks(taskStats.getInProgressTasks())
                .doneTasks(taskStats.getDoneTasks())
                .totalMembers(memberCount.intValue() + 1) // +1 for owner
                .build();
    }

    // Helper methods
    private boolean canAccessProject(Project project, Long userId, String role) {
        if ("ADMIN".equals(role)) {
            return true;
        }

        if (project.getOwnerId().equals(userId)) {
            return true;
        }

        return memberRepository.existsByProjectIdAndUserId(project.getId(), userId);
    }

    private ProjectResponse mapToProjectResponse(Project project, Long userId, String role) {
        List<MemberResponse> members = project.getMembers().stream()
                .map(member -> {
                    try {
                        UserDTO user = authServiceClient.getUserById(member.getUserId(), userId, role);
                        return mapToMemberResponse(member, user);
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(memberResponse -> memberResponse != null)
                .collect(Collectors.toList());

        return ProjectResponse.builder()
                .id(project.getId())
                .title(project.getTitle())
                .description(project.getDescription())
                .status(project.getStatus())
                .color(project.getColor())
                .startDate(project.getStartDate())
                .endDate(project.getEndDate())
                .ownerId(project.getOwnerId())
                .members(members)
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }

    private MemberResponse mapToMemberResponse(ProjectMember member, UserDTO user) {
        return MemberResponse.builder()
                .id(member.getId())
                .userId(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .joinedAt(member.getJoinedAt())
                .build();
    }
}
