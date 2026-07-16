package com.company.resourceallocation.core.project.dto;

import com.company.resourceallocation.core.project.entity.ProjectStatus;
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
public class ProjectResponse {

    Long projectId;
    String projectCode;
    String projectName;
    String customer;
    LocalDate startDate;
    LocalDate endDate;
    ProjectStatus status;
    LocalDateTime createdAt;
}
