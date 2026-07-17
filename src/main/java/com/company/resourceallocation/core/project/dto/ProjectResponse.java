package com.company.resourceallocation.core.project.dto;

import com.company.resourceallocation.core.project.entity.ProjectStatus;
import lombok.Builder;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
public record ProjectResponse(
    Long projectId,
    String projectCode,
    String projectName,
    String customer,
    LocalDate startDate,
    LocalDate endDate,
    ProjectStatus status,
    LocalDateTime createdAt
) {}
