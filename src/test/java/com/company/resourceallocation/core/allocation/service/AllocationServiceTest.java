package com.company.resourceallocation.core.allocation.service;
import com.company.resourceallocation.core.allocation.exception.InvalidAllocationPercentageException;
import com.company.resourceallocation.core.allocation.exception.AllocationExceededException;

import com.company.resourceallocation.core.allocation.entity.Allocation;
import com.company.resourceallocation.core.allocation.repository.AllocationRepository;
import com.company.resourceallocation.core.allocation.mapper.AllocationMapper;


import com.company.resourceallocation.core.allocation.dto.AllocationRequest;
import com.company.resourceallocation.core.allocation.dto.AllocationResponse;
import com.company.resourceallocation.core.employee.entity.Employee;
import com.company.resourceallocation.core.employee.repository.EmployeeRepository;
import com.company.resourceallocation.core.project.entity.Project;
import com.company.resourceallocation.core.project.repository.ProjectRepository;
import com.company.resourceallocation.core.project.entity.ProjectStatus;
import com.company.resourceallocation.core.project.exception.InvalidProjectStatusException;
import com.company.resourceallocation.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AllocationServiceTest {

    @Mock
    AllocationRepository allocationRepository;

    @Mock
    EmployeeRepository employeeRepository;

    @Mock
    ProjectRepository projectRepository;

    @Mock
    AllocationMapper allocationMapper;

    @InjectMocks
    AllocationService allocationService;

    Employee employee;
    Project project;
    AllocationRequest request;
    Allocation allocation;
    AllocationResponse response;

    @BeforeEach
    void setUp() {
        employee = Employee.builder().employeeId(1L).fullName("Tuan").build();
        project = Project.builder().projectId(2L).projectCode("NCG").status(ProjectStatus.ACTIVE).build();

        request = AllocationRequest.builder()
                .employeeId(1L)
                .projectId(2L)
                .allocationPercent(50)
                .startDate(LocalDate.of(2025, 1, 1))
                .endDate(LocalDate.of(2025, 6, 30))
                .build();

        allocation = Allocation.builder()
                .allocationId(10L)
                .employee(employee)
                .project(project)
                .allocationPercent(50)
                .build();

        response = AllocationResponse.builder()
                .allocationId(10L)
                .employeeId(1L)
                .projectId(2L)
                .allocationPercent(50)
                .build();
    }

    @Test
    void should_createAllocationSuccessfully_when_validRequest() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(projectRepository.findById(2L)).thenReturn(Optional.of(project));
        when(allocationRepository.sumAllocationByEmployeeExcluding(1L, -1L)).thenReturn(30);
        when(allocationMapper.toEntity(request)).thenReturn(allocation);
        when(allocationRepository.save(allocation)).thenReturn(allocation);
        when(allocationMapper.toResponse(allocation)).thenReturn(response);

        AllocationResponse result = allocationService.createAllocation(request);

        assertNotNull(result);
        assertEquals(50, result.getAllocationPercent());
        verify(allocationRepository).save(allocation);
    }

    @Test
    void should_throwInvalidAllocationPercentageException_when_percentIsInvalid() {
        request.setAllocationPercent(120);

        assertThrows(InvalidAllocationPercentageException.class, () -> allocationService.createAllocation(request));
    }

    @Test
    void should_throwIllegalArgumentException_when_endDateIsBeforeStartDate() {
        request.setEndDate(LocalDate.of(2024, 12, 31));

        assertThrows(IllegalArgumentException.class, () -> allocationService.createAllocation(request));
    }

    @Test
    void should_throwInvalidProjectStatusException_when_projectIsCompleted() {
        project.setStatus(ProjectStatus.COMPLETED);
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(projectRepository.findById(2L)).thenReturn(Optional.of(project));

        assertThrows(InvalidProjectStatusException.class, () -> allocationService.createAllocation(request));
    }

    @Test
    void should_throwAllocationExceededException_when_totalExceeds100() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(projectRepository.findById(2L)).thenReturn(Optional.of(project));
        when(allocationRepository.sumAllocationByEmployeeExcluding(1L, -1L)).thenReturn(70); // 70 + 50 = 120 > 100

        assertThrows(AllocationExceededException.class, () -> allocationService.createAllocation(request));
    }

    @Test
    void should_throwResourceNotFoundException_when_employeeNotFound() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> allocationService.createAllocation(request));
    }

    @Test
    void should_allowUpdate_when_existingAllocationIsExcludedFromCapacityCheck() {
        when(allocationRepository.findById(10L)).thenReturn(Optional.of(allocation));
        when(allocationRepository.sumAllocationByEmployeeExcluding(1L, 10L)).thenReturn(30);
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(projectRepository.findById(2L)).thenReturn(Optional.of(project));
        when(allocationRepository.save(any(Allocation.class))).thenReturn(allocation);
        when(allocationMapper.toResponse(allocation)).thenReturn(response);

        AllocationResponse result = allocationService.updateAllocation(10L, request);

        assertNotNull(result);
        verify(allocationRepository).save(any(Allocation.class));
    }

    @Test
    void should_returnAllocations_when_filteringByEmployeeAndProject() {
        Pageable pageable = Pageable.ofSize(10);
        when(allocationRepository.findFiltered(1L, 2L, pageable)).thenReturn(new PageImpl<>(List.of(allocation), pageable, 1));
        when(allocationMapper.toResponse(allocation)).thenReturn(response);

        Page<AllocationResponse> result = allocationService.getAllocations(1L, 2L, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(10L, result.getContent().get(0).getAllocationId());
    }
}
