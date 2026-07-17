package com.company.resourceallocation.report.dto;

import lombok.Builder;

@Builder
public record AvailableResponse(
    Long employeeId,
    String employeeName,
    Integer available
) {}
