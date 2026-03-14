package com.example.projectservice.services;

import com.example.projectservice.client.AuthServiceClient;
import com.example.projectservice.client.TaskServiceClient;
import com.example.projectservice.dto.ProjectRequest;
import com.example.projectservice.dto.ProjectResponse;
import com.example.projectservice.dto.UserDTO;
import com.example.projectservice.entity.Project;
import com.example.projectservice.entity.ProjectStatus;
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

import java.time.LocalDateTime;
import java.util.Collections;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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


}
