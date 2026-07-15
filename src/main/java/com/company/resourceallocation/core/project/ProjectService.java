package com.company.resourceallocation.core.project;

import com.company.resourceallocation.core.project.dto.ProjectRequest;
import com.company.resourceallocation.core.project.dto.ProjectResponse;
import com.company.resourceallocation.core.allocation.AllocationRepository;
import com.company.resourceallocation.exception.DuplicateResourceException;
import com.company.resourceallocation.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.AccessLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProjectService {

    ProjectRepository projectRepository;
    ProjectMapper projectMapper;
    AllocationRepository allocationRepository;

    @Transactional
    public ProjectResponse createProject(ProjectRequest request) {
        validateDates(request);

        if (projectRepository.existsByProjectCode(request.getProjectCode())) {
            throw new DuplicateResourceException("Project code " + request.getProjectCode() + " already exists");
        }

        Project project = projectMapper.toEntity(request);
        if (project.getStatus() == null) {
            project.setStatus(ProjectStatus.PLANNING);
        }

        Project saved = projectRepository.save(project);
        return projectMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<ProjectResponse> getProjects(ProjectStatus status, Pageable pageable) {
        Page<Project> projects = projectRepository.findFiltered(status, pageable);
        return projects.map(projectMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProjectById(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project", id));
        return projectMapper.toResponse(project);
    }

    @Transactional
    public ProjectResponse updateProject(Long id, ProjectRequest request) {
        validateDates(request);

        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project", id));

        if (projectRepository.existsByProjectCodeAndProjectIdNot(request.getProjectCode(), id)) {
            throw new DuplicateResourceException("Project code " + request.getProjectCode() + " already exists");
        }

        projectMapper.updateEntity(request, project);
        if (project.getStatus() == null) {
            project.setStatus(ProjectStatus.PLANNING);
        }

        Project saved = projectRepository.save(project);
        return projectMapper.toResponse(saved);
    }

    @Transactional
    public void deleteProject(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project", id));

        if (allocationRepository.existsByProjectProjectId(id)) {
            throw new ProjectInUseException("Cannot delete project: still has active allocations");
        }

        projectRepository.delete(project);
    }

    private void validateDates(ProjectRequest request) {
        if (request.getStartDate() != null && request.getEndDate() != null) {
            if (request.getEndDate().isBefore(request.getStartDate())) {
                throw new IllegalArgumentException("End date must be on or after start date");
            }
        }
    }
}
