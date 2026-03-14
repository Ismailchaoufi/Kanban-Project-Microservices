package com.example.projectservice.services;

import com.example.projectservice.client.AuthServiceClient;
import com.example.projectservice.client.TaskServiceClient;
import com.example.projectservice.dto.UserDTO;
import com.example.projectservice.entity.Project;
import com.example.projectservice.repository.ProjectMemberRepository;
import com.example.projectservice.repository.ProjectRepository;
import com.example.projectservice.service.ProjectService;
import com.example.projectservice.service.TaskStatusService;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
}
