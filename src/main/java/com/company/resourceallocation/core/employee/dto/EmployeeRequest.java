package com.company.resourceallocation.core.employee.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder(toBuilder = true)
public record EmployeeRequest(
    @NotBlank(message = "Employee code is required")
    String employeeCode,

    @NotBlank(message = "Full name is required")
    String fullName,

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    String email,

    @NotBlank(message = "Role is required")
    String role,

    String department
) {}
