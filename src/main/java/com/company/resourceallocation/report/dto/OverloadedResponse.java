package com.company.resourceallocation.report.dto;

import lombok.Builder;

@Builder
public record OverloadedResponse(
    Long employeeId,
    String employeeName,
    Integer totalAllocation
) {}
