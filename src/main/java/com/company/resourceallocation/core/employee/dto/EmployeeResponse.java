package com.company.resourceallocation.core.employee.dto;

import lombok.Builder;
import java.time.LocalDateTime;

@Builder
public record EmployeeResponse(
    Long employeeId,
    String employeeCode,
    String fullName,
    String email,
    String role,
    String department,
    LocalDateTime createdAt
) {}
