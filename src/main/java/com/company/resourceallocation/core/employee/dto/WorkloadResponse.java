package com.company.resourceallocation.core.employee.dto;

import lombok.Builder;
import java.util.List;

@Builder
public record WorkloadResponse(
    Long employeeId,
    String employeeName,
    Integer allocated,
    Integer available,
    List<AllocationBreakdown> allocations
) {
    @Builder
    public record AllocationBreakdown(
        String projectCode,
        Integer allocationPercent
    ) {}
}
