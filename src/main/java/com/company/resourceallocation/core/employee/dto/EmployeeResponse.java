package com.company.resourceallocation.core.employee.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EmployeeResponse {

    Long employeeId;
    String employeeCode;
    String fullName;
    String email;
    String role;
    String department;
    LocalDateTime createdAt;
}
