package com.company.resourceallocation.core.project.dto;

import com.company.resourceallocation.core.project.entity.ProjectStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import java.time.LocalDate;

@Builder(toBuilder = true)
public record ProjectRequest(
    @NotBlank(message = "PROJECT_CODE_REQUIRED")
    String projectCode,

    @NotBlank(message = "PROJECT_NAME_REQUIRED")
    String projectName,

    String customer,

    LocalDate startDate,

    LocalDate endDate,

    ProjectStatus status
) {}
