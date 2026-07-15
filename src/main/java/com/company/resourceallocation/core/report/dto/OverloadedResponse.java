package com.company.resourceallocation.core.report.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OverloadedResponse {
    Long employeeId;
    String employeeName;
    Integer totalAllocation;
}
