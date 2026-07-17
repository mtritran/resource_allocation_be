package com.company.resourceallocation.core.allocation.dto;

import lombok.Builder;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.company.resourceallocation.core.allocation.entity.AllocationStatus;

@Builder
public record AllocationResponse(
        Long allocationId,
        Long employeeId,
        String employeeName,
        Long projectId,
        String projectCode,
        Integer allocationPercent,
        String roleInProject,
        LocalDate startDate,
        LocalDate endDate,
        AllocationStatus status,
        LocalDateTime createdAt) {
}
