package com.company.resourceallocation.core.allocation.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AllocationResponse {

    Long allocationId;
    Long employeeId;
    String employeeName;
    Long projectId;
    String projectCode;
    Integer allocationPercent;
    String roleInProject;
    LocalDate startDate;
    LocalDate endDate;
    com.company.resourceallocation.core.allocation.entity.AllocationStatus status;
    LocalDateTime createdAt;
}
