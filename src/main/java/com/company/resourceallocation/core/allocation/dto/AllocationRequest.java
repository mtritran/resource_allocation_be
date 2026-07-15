package com.company.resourceallocation.core.allocation.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AllocationRequest {

    @NotNull(message = "Employee ID is required")
    Long employeeId;

    @NotNull(message = "Project ID is required")
    Long projectId;

    @NotNull(message = "Allocation percent is required")
    @Min(value = 1, message = "Allocation percent must be at least 1")
    @Max(value = 100, message = "Allocation percent cannot exceed 100")
    Integer allocationPercent;

    String roleInProject;

    LocalDate startDate;

    LocalDate endDate;
}
