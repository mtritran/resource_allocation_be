package com.company.resourceallocation.core.employee.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder(toBuilder = true)
public record EmployeeRequest(
    @NotBlank(message = "EMPLOYEE_CODE_REQUIRED")
    String employeeCode,

    @NotBlank(message = "FULL_NAME_REQUIRED")
    String fullName,

    @NotBlank(message = "EMAIL_REQUIRED")
    @Email(message = "INVALID_EMAIL_FORMAT")
    String email,

    @NotBlank(message = "ROLE_REQUIRED")
    String role,

    String department
) {}
