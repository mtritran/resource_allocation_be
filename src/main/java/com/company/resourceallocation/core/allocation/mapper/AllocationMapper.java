package com.company.resourceallocation.core.allocation.mapper;
import com.company.resourceallocation.core.allocation.entity.Allocation;
import com.company.resourceallocation.core.allocation.dto.AllocationRequest;
import com.company.resourceallocation.core.allocation.dto.AllocationResponse;


import com.company.resourceallocation.core.allocation.dto.AllocationRequest;
import com.company.resourceallocation.core.allocation.dto.AllocationResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface AllocationMapper {

    @Mapping(target = "allocationId", ignore = true)
    @Mapping(target = "employee", ignore = true)
    @Mapping(target = "project", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Allocation toEntity(AllocationRequest request);

    @Mapping(source = "employee.employeeId", target = "employeeId")
    @Mapping(source = "employee.fullName", target = "employeeName")
    @Mapping(source = "project.projectId", target = "projectId")
    @Mapping(source = "project.projectCode", target = "projectCode")
    AllocationResponse toResponse(Allocation entity);

    @Mapping(target = "allocationId", ignore = true)
    @Mapping(target = "employee", ignore = true)
    @Mapping(target = "project", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(AllocationRequest request, @MappingTarget Allocation entity);
}
