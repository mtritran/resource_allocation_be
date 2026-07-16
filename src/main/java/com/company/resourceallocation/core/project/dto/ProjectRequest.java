package com.company.resourceallocation.core.project.dto;

import com.company.resourceallocation.core.project.entity.ProjectStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProjectRequest {

    @NotBlank(message = "Project code is required")
    String projectCode;

    @NotBlank(message = "Project name is required")
    String projectName;

    String customer;

    LocalDate startDate;

    LocalDate endDate;

    ProjectStatus status;
}
