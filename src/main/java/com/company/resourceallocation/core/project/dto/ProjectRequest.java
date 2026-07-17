package com.company.resourceallocation.core.project.dto;

import com.company.resourceallocation.core.project.entity.ProjectStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import java.time.LocalDate;

@Builder(toBuilder = true)
public record ProjectRequest(
    @NotBlank(message = "Project code is required")
    String projectCode,

    @NotBlank(message = "Project name is required")
    String projectName,

    String customer,

    LocalDate startDate,

    LocalDate endDate,

    ProjectStatus status
) {}
