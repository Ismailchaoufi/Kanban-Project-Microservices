package com.example.projectservice.services;

import com.example.projectservice.client.AuthServiceClient;
import com.example.projectservice.client.TaskServiceClient;
import com.example.projectservice.dto.*;
import com.example.projectservice.entity.Project;
import com.example.projectservice.entity.ProjectMember;
import com.example.projectservice.entity.ProjectStatus;
import com.example.projectservice.entity.TaskStatsDTO;
import com.example.projectservice.exception.BadRequestException;
import com.example.projectservice.exception.ForbiddenException;
import com.example.projectservice.exception.ResourceNotFoundException;
import com.example.projectservice.repository.ProjectMemberRepository;
import com.example.projectservice.repository.ProjectRepository;
import com.example.projectservice.service.ProjectService;
import com.example.projectservice.service.TaskStatusService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProjectServiceTest {
    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectMemberRepository memberRepository;

    @Mock
    private AuthServiceClient authServiceClient;

    @Mock
    private TaskServiceClient taskServiceClient;

    @Mock
    private TaskStatusService taskStatusService;

    @InjectMocks
    private ProjectService projectService;

    private Project sampleProject;
    private UserDTO sampleUser;

    @BeforeEach
    void setUp() {
        sampleProject = new Project();
        sampleProject.setId(1L);
        sampleProject.setTitle("Mon projet");
        sampleProject.setDescription("Description");
        sampleProject.setStatus(ProjectStatus.IN_PROGRESS);
        sampleProject.setOwnerId(10L);
        sampleProject.setMembers(Collections.emptySet());
        sampleProject.setCreatedAt(LocalDateTime.now());
        sampleProject.setUpdatedAt(LocalDateTime.now());

        sampleUser = new UserDTO();
        sampleUser.setId(10L);
        sampleUser.setFirstName("Alice");
        sampleUser.setLastName("Martin");
        sampleUser.setEmail("alice@example.com");
    }

    // tests - createProject
    @Test
    void createProject_devrait_sauvegarder_et_retourner_le_projet() {
        ProjectRequest request = new ProjectRequest();
        request.setTitle("Mon projet");
        request.setDescription("Description");

        when(projectRepository.save(any(Project.class))).thenReturn(sampleProject);
        when(authServiceClient.getUserByIdInternal(10L)).thenReturn(sampleUser);

        ProjectResponse response = projectService.createProject(request, 10L);

        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo("Mon projet");
        verify(taskStatusService).initializeDefaultStatuses(1L);
    }

    @Test
    void createProject_utilise_IN_PROGRESS_par_defaut_si_statut_null() {
        ProjectRequest request = new ProjectRequest();
        request.setTitle("Projet sans statut");
        request.setStatus(null);

        when(projectRepository.save(any(Project.class))).thenAnswer(inv -> {
            Project p = inv.getArgument(0);
            p.setId(2L);
            p.setMembers(Collections.emptySet());
            p.setCreatedAt(LocalDateTime.now());
            p.setUpdatedAt(LocalDateTime.now());
            return p;
        });
        when(authServiceClient.getUserByIdInternal(anyLong())).thenReturn(sampleUser);

        ProjectResponse response = projectService.createProject(request, 10L);

        assertThat(response.getStatus()).isEqualTo(ProjectStatus.IN_PROGRESS);
    }

    // getAllProjects
    @Test
    void getAllProjects_admin_voit_tous_les_projets() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Project> page = new PageImpl<>(List.of(sampleProject));

        when(projectRepository.findAll(pageable)).thenReturn(page);
        when(authServiceClient.getUserByIdInternal(anyLong())).thenReturn(sampleUser);

        Page<ProjectResponse> result = projectService.getAllProjects(99L, "ADMIN", pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(projectRepository).findAll(pageable);
        verify(projectRepository, never()).findByOwnerIdOrMemberId(anyLong(), any());
    }

    @Test
    void getAllProjects_user_voit_seulement_ses_projets() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Project> page = new PageImpl<>(List.of(sampleProject));

        when(projectRepository.findByOwnerIdOrMemberId(10L, pageable)).thenReturn(page);
        when(authServiceClient.getUserByIdInternal(anyLong())).thenReturn(sampleUser);

        Page<ProjectResponse> result = projectService.getAllProjects(10L, "USER", pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(projectRepository).findByOwnerIdOrMemberId(10L, pageable);
        verify(projectRepository, never()).findAll(pageable);
    }

    // getProjectById
    @Test
    void getProjectById_owner_peut_acceder_son_projet() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject));
        when(authServiceClient.getUserByIdInternal(anyLong())).thenReturn(sampleUser);

        ProjectResponse response = projectService.getProjectById(1L, 10L, "USER");

        assertThat(response.getId()).isEqualTo(1L);
    }

    @Test
    void getProjectById_lance_exception_si_projet_introuvable() {
        when(projectRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.getProjectById(999L, 10L, "USER"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Project not found");
    }

    @Test
    void getProjectById_lance_exception_si_utilisateur_non_autorise() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject));
        when(memberRepository.existsByProjectIdAndUserId(1L, 99L)).thenReturn(false);

        // userId=99 n'est ni owner ni membre ni admin
        assertThatThrownBy(() -> projectService.getProjectById(1L, 99L, "USER"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void getProjectById_admin_peut_acceder_nimporte_quel_projet() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject));
        when(authServiceClient.getUserByIdInternal(anyLong())).thenReturn(sampleUser);

        // userId=99 n'est pas owner, mais est ADMIN
        ProjectResponse response = projectService.getProjectById(1L, 99L, "ADMIN");

        assertThat(response.getId()).isEqualTo(1L);
    }

    // updateProject
    @Test
    void updateProject_owner_peut_modifier_son_projet() {
        ProjectRequest request = new ProjectRequest();
        request.setTitle("Nouveau titre");

        when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject));
        when(projectRepository.save(any())).thenReturn(sampleProject);
        when(authServiceClient.getUserByIdInternal(anyLong())).thenReturn(sampleUser);

        ProjectResponse response = projectService.updateProject(1L, request, 10L, "USER");

        assertThat(response).isNotNull();
        verify(projectRepository).save(sampleProject);
    }

    @Test
    void updateProject_refuse_si_pas_owner_ni_admin() {
        ProjectRequest request = new ProjectRequest();
        request.setTitle("Tentative de modification");

        when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject));

        assertThatThrownBy(() -> projectService.updateProject(1L, request, 99L, "USER"))
                .isInstanceOf(ForbiddenException.class);

        verify(projectRepository, never()).save(any());
    }

    @Test
    void updateProject_ne_modifie_pas_les_champs_null() {
        ProjectRequest request = new ProjectRequest();
        request.setTitle(null);

        when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject));
        when(projectRepository.save(any())).thenReturn(sampleProject);
        when(authServiceClient.getUserByIdInternal(anyLong())).thenReturn(sampleUser);

        projectService.updateProject(1L, request, 10L, "USER");


        assertThat(sampleProject.getTitle()).isEqualTo("Mon projet");
    }

    // deleteProject
    @Test
    void deleteProject_owner_peut_supprimer_son_projet() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject));

        projectService.deleteProject(1L, 10L, "USER");

        verify(projectRepository).delete(sampleProject);
    }

    @Test
    void deleteProject_refuse_si_pas_owner_ni_admin() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject));

        assertThatThrownBy(() -> projectService.deleteProject(1L, 99L, "USER"))
                .isInstanceOf(ForbiddenException.class);

        verify(projectRepository, never()).delete(any());
    }

    //Add member
    @Test
    void addMember_owner_peut_ajouter_un_membre() {
        AddMemberRequest request = new AddMemberRequest();
        request.setUserId(20L);

        ProjectMember savedMember = new ProjectMember();
        savedMember.setId(5L);
        savedMember.setUserId(20L);
        savedMember.setProject(sampleProject);
        savedMember.setJoinedAt(LocalDateTime.now());

        UserDTO newUser = new UserDTO();
        newUser.setId(20L);
        newUser.setFirstName("Bob");
        newUser.setLastName("Dupont");
        newUser.setEmail("bob@example.com");

        when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject));
        when(memberRepository.existsByProjectIdAndUserId(1L, 20L)).thenReturn(false);
        when(authServiceClient.getUserById(eq(20L), anyLong(), anyString())).thenReturn(newUser);
        when(memberRepository.save(any())).thenReturn(savedMember);

        MemberResponse response = projectService.addMember(1L, request, 10L, "USER");
        assertThat(response.getUserId()).isEqualTo(20L);
        assertThat(response.getFirstName()).isEqualTo("Bob");
    }

    @Test
    void addMember_refuse_si_deja_membre() {
        AddMemberRequest request = new AddMemberRequest();
        request.setUserId(20L);

        when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject));
        when(memberRepository.existsByProjectIdAndUserId(1L, 20L)).thenReturn(true);

        assertThatThrownBy(() -> projectService.addMember(1L, request, 10L, "USER"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already a member");
    }

    @Test
    void addMember_refuse_si_pas_owner_ni_admin() {
        AddMemberRequest request = new AddMemberRequest();
        request.setUserId(20L);

        when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject));

        assertThatThrownBy(() -> projectService.addMember(1L, request, 99L, "USER"))
                .isInstanceOf(ForbiddenException.class);
    }

    // =========================================================
    // removeMember
    // =========================================================

    @Test
    void removeMember_owner_peut_retirer_un_membre() {
        ProjectMember member = new ProjectMember();
        member.setId(5L);
        member.setUserId(20L);

        when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject));
        when(memberRepository.findByProjectIdAndUserId(1L, 20L)).thenReturn(Optional.of(member));

        projectService.removeMember(1L, 20L, 10L, "USER");

        verify(memberRepository).delete(member);
    }

    @Test
    void removeMember_impossible_de_retirer_le_owner() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject));

        // Le owner essaie de se retirer lui-même
        assertThatThrownBy(() -> projectService.removeMember(1L, 10L, 10L, "USER"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot remove the project owner");
    }

    // =========================================================
    // getProjectStats
    // =========================================================

    @Test
    void getProjectStats_retourne_les_stats_correctes() {
        TaskStatsDTO taskStats = TaskStatsDTO.builder()
                .projectId(1L)
                .totalTasks(10)
                .todoTasks(3)
                .inProgressTasks(4)
                .doneTasks(3)
                .build();

        when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject));
        when(taskServiceClient.getTaskStatsByProject(1L)).thenReturn(taskStats);
        when(memberRepository.countByProjectId(1L)).thenReturn(4L);

        ProjectStatsResponse stats = projectService.getProjectStats(1L, 10L, "USER");

        assertThat(stats.getTotalTasks()).isEqualTo(10);
        assertThat(stats.getTotalMembers()).isEqualTo(5); // 4 membres + 1 owner
    }

    @Test
    void getProjectStats_retourne_stats_vides_si_task_service_echoue() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject));
        when(taskServiceClient.getTaskStatsByProject(1L)).thenThrow(new RuntimeException("Service indisponible"));
        when(memberRepository.countByProjectId(1L)).thenReturn(2L);

        // Ne doit pas planter — renvoie des stats à zéro
        ProjectStatsResponse stats = projectService.getProjectStats(1L, 10L, "USER");

        assertThat(stats.getTotalTasks()).isEqualTo(0);
        assertThat(stats.getTotalMembers()).isEqualTo(3); // 2 membres + 1 owner
    }


}
