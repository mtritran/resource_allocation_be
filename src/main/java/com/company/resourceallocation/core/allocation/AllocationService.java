package com.company.resourceallocation.core.allocation;

import com.company.resourceallocation.core.allocation.dto.AllocationRequest;
import com.company.resourceallocation.core.allocation.dto.AllocationResponse;
import com.company.resourceallocation.core.employee.Employee;
import com.company.resourceallocation.core.employee.EmployeeRepository;
import com.company.resourceallocation.core.project.Project;
import com.company.resourceallocation.core.project.ProjectRepository;
import com.company.resourceallocation.core.project.ProjectStatus;
import com.company.resourceallocation.core.project.InvalidProjectStatusException;
import com.company.resourceallocation.exception.*;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.AccessLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AllocationService {

    AllocationRepository allocationRepository;
    EmployeeRepository employeeRepository;
    ProjectRepository projectRepository;
    AllocationMapper allocationMapper;

    @Transactional
    public AllocationResponse createAllocation(AllocationRequest request) {
        validateRequestRules(request, -1L);

        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee", request.getEmployeeId()));

        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project", request.getProjectId()));

        Allocation allocation = allocationMapper.toEntity(request);
        allocation.setEmployee(employee);
        allocation.setProject(project);

        Allocation saved = allocationRepository.save(allocation);
        return allocationMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<AllocationResponse> getAllocations(Long employeeId, Long projectId, Pageable pageable) {
        Page<Allocation> allocations = allocationRepository.findFiltered(employeeId, projectId, pageable);
        return allocations.map(allocationMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public AllocationResponse getAllocationById(Long id) {
        Allocation allocation = allocationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Allocation", id));
        return allocationMapper.toResponse(allocation);
    }

    @Transactional
    public AllocationResponse updateAllocation(Long id, AllocationRequest request) {
        Allocation allocation = allocationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Allocation", id));

        validateRequestRules(request, id);

        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee", request.getEmployeeId()));

        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project", request.getProjectId()));

        allocationMapper.updateEntity(request, allocation);
        allocation.setEmployee(employee);
        allocation.setProject(project);

        Allocation saved = allocationRepository.save(allocation);
        return allocationMapper.toResponse(saved);
    }

    @Transactional
    public void deleteAllocation(Long id) {
        Allocation allocation = allocationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Allocation", id));
        allocationRepository.delete(allocation);
    }

    private void validateRequestRules(AllocationRequest request, Long excludeId) {
        // BR-ALC-01
        if (request.getAllocationPercent() < 1 || request.getAllocationPercent() > 100) {
            throw new InvalidAllocationPercentException("Allocation percent must be between 1 and 100");
        }

        // BR-ALC-06
        if (request.getStartDate() != null && request.getEndDate() != null) {
            if (request.getEndDate().isBefore(request.getStartDate())) {
                throw new IllegalArgumentException("End date must be on or after start date");
            }
        }

        // BR-ALC-04
        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee", request.getEmployeeId()));

        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project", request.getProjectId()));

        // BR-ALC-03 / BR-PRJ-03
        if (ProjectStatus.COMPLETED.equals(project.getStatus())) {
            throw new InvalidProjectStatusException("Cannot allocate to a COMPLETED project");
        }

        // BR-ALC-02
        int currentSum = allocationRepository.sumAllocationByEmployeeExcluding(request.getEmployeeId(), excludeId);
        if (currentSum + request.getAllocationPercent() > 100) {
            throw new AllocationExceededException("Employee allocation exceeds 100%");
        }
    }
}
