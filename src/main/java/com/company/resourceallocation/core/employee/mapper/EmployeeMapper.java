package com.company.resourceallocation.core.employee.mapper;

import com.company.resourceallocation.core.employee.entity.Employee;
import com.company.resourceallocation.core.employee.dto.EmployeeRequest;
import com.company.resourceallocation.core.employee.dto.EmployeeResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface EmployeeMapper {

    @Mapping(target = "employeeId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Employee toEntity(EmployeeRequest request);

    EmployeeResponse toResponse(Employee entity);

    @Mapping(target = "employeeId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(EmployeeRequest request, @MappingTarget Employee entity);
}
