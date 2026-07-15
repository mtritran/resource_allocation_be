package com.company.resourceallocation.core.employee.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WorkloadResponse {

    Long employeeId;
    String employeeName;
    Integer totalAllocation;
    Integer available;
    List<AllocationBreakdown> allocations;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class AllocationBreakdown {
        String projectCode;
        Integer allocationPercent;
    }
}
