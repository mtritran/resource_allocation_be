package com.company.resourceallocation.core.employee.dto;

import lombok.Builder;

@Builder
public record EmployeeSkillSearchResponse(
    String employeeName,
    Integer available
) {}
