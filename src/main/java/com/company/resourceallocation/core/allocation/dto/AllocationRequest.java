package com.company.resourceallocation.core.allocation.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import java.time.LocalDate;

@Builder(toBuilder = true)
public record AllocationRequest(
    @NotNull(message = "EMPLOYEE_ID_REQUIRED")
    Long employeeId,

    @NotNull(message = "PROJECT_ID_REQUIRED")
    Long projectId,

    @NotNull(message = "ALLOCATION_PERCENT_REQUIRED")
    @Min(value = 1, message = "ALLOCATION_PERCENT_MIN_1")
    @Max(value = 100, message = "ALLOCATION_PERCENT_MAX_100")
    Integer allocationPercent,

    String roleInProject,

    LocalDate startDate,

    LocalDate endDate
) {}
