package com.company.resourceallocation.core.employee.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EmployeeSkillSearchResponse {
    String employeeName;
    Integer available;
}
