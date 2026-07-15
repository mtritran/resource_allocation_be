package com.company.resourceallocation.core.allocation;

import com.company.resourceallocation.core.allocation.dto.AllocationRequest;
import com.company.resourceallocation.core.allocation.dto.AllocationResponse;
import com.company.resourceallocation.core.employee.Employee;
import com.company.resourceallocation.core.employee.EmployeeRepository;
import com.company.resourceallocation.core.project.Project;
import com.company.resourceallocation.core.project.ProjectRepository;
import com.company.resourceallocation.core.project.ProjectStatus;
import com.company.resourceallocation.exception.AllocationExceededException;
import com.company.resourceallocation.exception.InvalidAllocationPercentException;
import com.company.resourceallocation.exception.InvalidProjectStatusException;
import com.company.resourceallocation.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDate;
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
    void should_throwInvalidAllocationPercentException_when_percentIsInvalid() {
        request.setAllocationPercent(120);

        assertThrows(InvalidAllocationPercentException.class, () -> allocationService.createAllocation(request));
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
}
