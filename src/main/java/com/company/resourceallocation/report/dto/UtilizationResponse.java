package com.company.resourceallocation.report.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UtilizationResponse {
    Long employeeId;
    String employeeName;
    Integer totalAllocation;
}
