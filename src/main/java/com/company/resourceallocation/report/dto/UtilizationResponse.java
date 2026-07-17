package com.company.resourceallocation.report.dto;

import lombok.Builder;

@Builder
public record UtilizationResponse(
    Long employeeId,
    String employeeName,
    Integer totalAllocation
) {}
