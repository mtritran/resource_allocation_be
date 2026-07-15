package com.company.resourceallocation.core.employee.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeResponse {

    private Long employeeId;
    private String employeeCode;
    private String fullName;
    private String email;
    private String role;
    private String department;
    private LocalDateTime createdAt;
}
