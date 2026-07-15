package com.company.resourceallocation.core.project;

import com.company.resourceallocation.core.project.dto.ProjectRequest;
import com.company.resourceallocation.core.project.dto.ProjectResponse;
import com.company.resourceallocation.exception.DuplicateResourceException;
import com.company.resourceallocation.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDate;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProjectServiceTest {

    @Mock
    ProjectRepository projectRepository;

    @Mock
    ProjectMapper projectMapper;

    @InjectMocks
    ProjectService projectService;

    ProjectRequest request;
    Project project;
    ProjectResponse response;

    @BeforeEach
    void setUp() {
        request = ProjectRequest.builder()
                .projectCode("NCG")
                .projectName("New Core Gateway")
                .customer("ABC Corp")
                .startDate(LocalDate.of(2025, 1, 1))
                .endDate(LocalDate.of(2025, 12, 31))
                .status(ProjectStatus.PLANNING)
                .build();

        project = Project.builder()
                .projectId(1L)
                .projectCode("NCG")
                .projectName("New Core Gateway")
                .customer("ABC Corp")
                .startDate(LocalDate.of(2025, 1, 1))
                .endDate(LocalDate.of(2025, 12, 31))
                .status(ProjectStatus.PLANNING)
                .build();

        response = ProjectResponse.builder()
                .projectId(1L)
                .projectCode("NCG")
                .projectName("New Core Gateway")
                .customer("ABC Corp")
                .startDate(LocalDate.of(2025, 1, 1))
                .endDate(LocalDate.of(2025, 12, 31))
                .status(ProjectStatus.PLANNING)
                .build();
    }

    @Test
    void should_createProjectSuccessfully_when_validRequest() {
        when(projectRepository.existsByProjectCode(request.getProjectCode())).thenReturn(false);
        when(projectMapper.toEntity(request)).thenReturn(project);
        when(projectRepository.save(project)).thenReturn(project);
        when(projectMapper.toResponse(project)).thenReturn(response);

        ProjectResponse result = projectService.createProject(request);

        assertNotNull(result);
        assertEquals("NCG", result.getProjectCode());
        verify(projectRepository).save(project);
    }

    @Test
    void should_throwDuplicateResourceException_when_duplicateProjectCode() {
        when(projectRepository.existsByProjectCode(request.getProjectCode())).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> projectService.createProject(request));
        verify(projectRepository, never()).save(any());
    }

    @Test
    void should_throwIllegalArgumentException_when_endDateIsBeforeStartDate() {
        request.setEndDate(LocalDate.of(2024, 12, 31)); // before 2025-01-01

        assertThrows(IllegalArgumentException.class, () -> projectService.createProject(request));
        verify(projectRepository, never()).save(any());
    }

    @Test
    void should_throwResourceNotFoundException_when_getProjectByIdNotExists() {
        when(projectRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> projectService.getProjectById(999L));
    }

    @Test
    void should_setPlanningStatus_when_statusNotProvided() {
        project.setStatus(null);
        when(projectMapper.toEntity(request)).thenReturn(project);
        when(projectRepository.save(project)).thenReturn(project);
        when(projectMapper.toResponse(project)).thenReturn(response);

        ProjectResponse result = projectService.createProject(request);

        assertEquals(ProjectStatus.PLANNING, result.getStatus());
    }
}
