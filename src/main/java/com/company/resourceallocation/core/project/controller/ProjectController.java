package com.company.resourceallocation.core.project.controller;
import com.company.resourceallocation.core.project.service.ProjectService;
import com.company.resourceallocation.core.project.dto.ProjectRequest;
import com.company.resourceallocation.core.project.dto.ProjectResponse;
import com.company.resourceallocation.core.project.entity.ProjectStatus;


import com.company.resourceallocation.core.project.dto.ProjectRequest;
import com.company.resourceallocation.core.project.dto.ProjectResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
@Tag(name = "Project API", description = "Endpoints for managing projects")
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    @Operation(summary = "Create Project", description = "Create a new project and validate project code and dates")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Project created successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "409", description = "Duplicate project code")
    })
    public ResponseEntity<ProjectResponse> createProject(@Parameter(description = "Project payload") @Valid @RequestBody ProjectRequest request) {
        ProjectResponse response = projectService.createProject(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Get All Projects", description = "List projects with optional status filter")
    public ResponseEntity<Page<ProjectResponse>> getProjects(
            @Parameter(description = "Filter by project status") @RequestParam(required = false) ProjectStatus status,
            Pageable pageable) {
        Page<ProjectResponse> response = projectService.getProjects(status, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Project by ID")
    public ResponseEntity<ProjectResponse> getProjectById(@Parameter(description = "Project identifier") @PathVariable Long id) {
        ProjectResponse response = projectService.getProjectById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update Project")
    public ResponseEntity<ProjectResponse> updateProject(
            @Parameter(description = "Project identifier") @PathVariable Long id,
            @Parameter(description = "Updated project payload") @Valid @RequestBody ProjectRequest request) {
        ProjectResponse response = projectService.updateProject(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete Project")
    public ResponseEntity<Void> deleteProject(@Parameter(description = "Project identifier") @PathVariable Long id) {
        projectService.deleteProject(id);
        return ResponseEntity.noContent().build();
    }
}
