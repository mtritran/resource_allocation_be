package com.company.resourceallocation.core.employee.service;

import com.company.resourceallocation.core.employee.entity.Employee;
import com.company.resourceallocation.core.employee.repository.EmployeeRepository;
import com.company.resourceallocation.core.employee.mapper.EmployeeMapper;
import com.company.resourceallocation.core.employee.dto.EmployeeRequest;
import com.company.resourceallocation.core.employee.dto.EmployeeResponse;
import com.company.resourceallocation.core.employee.dto.WorkloadResponse;
import com.company.resourceallocation.core.allocation.entity.Allocation;
import com.company.resourceallocation.core.allocation.repository.AllocationRepository;
import com.company.resourceallocation.core.employee.exception.EmployeeInUseException;
import com.company.resourceallocation.core.skill.entity.Skill;
import com.company.resourceallocation.core.skill.repository.SkillRepository;
import com.company.resourceallocation.core.skill.dto.SkillResponse;
import com.company.resourceallocation.core.skill.mapper.SkillMapper;
import com.company.resourceallocation.core.employee.dto.EmployeeSkillSearchResponse;
import com.company.resourceallocation.exception.DuplicateResourceException;
import com.company.resourceallocation.exception.ResourceNotFoundException;
import java.util.List;
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
public class EmployeeService {

    EmployeeRepository employeeRepository;
    EmployeeMapper employeeMapper;
    AllocationRepository allocationRepository;
    SkillRepository skillRepository;
    SkillMapper skillMapper;

    @Transactional
    public EmployeeResponse createEmployee(EmployeeRequest request) {
        if (employeeRepository.existsByEmployeeCode(request.employeeCode())) {
            throw new DuplicateResourceException("Employee code " + request.employeeCode() + " already exists");
        }
        if (employeeRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email " + request.email() + " already exists");
        }

        Employee employee = employeeMapper.toEntity(request);
        Employee saved = employeeRepository.save(employee);
        return employeeMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<EmployeeResponse> getEmployees(String department, String role, Pageable pageable) {
        Page<Employee> employees = employeeRepository.findFiltered(department, role, pageable);
        return employees.map(employeeMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public EmployeeResponse getEmployeeById(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", id));
        return employeeMapper.toResponse(employee);
    }

    @Transactional
    public EmployeeResponse updateEmployee(Long id, EmployeeRequest request) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", id));

        if (employeeRepository.existsByEmployeeCodeAndEmployeeIdNot(request.employeeCode(), id)) {
            throw new DuplicateResourceException("Employee code " + request.employeeCode() + " already exists");
        }
        if (employeeRepository.existsByEmailAndEmployeeIdNot(request.email(), id)) {
            throw new DuplicateResourceException("Email " + request.email() + " already exists");
        }

        employeeMapper.updateEntity(request, employee);
        Employee saved = employeeRepository.save(employee);
        return employeeMapper.toResponse(saved);
    }

    @Transactional
    public void deleteEmployee(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", id));

        if (allocationRepository.existsByEmployeeEmployeeId(id)) {
            throw new EmployeeInUseException("Cannot delete employee: still has active allocations");
        }

        employeeRepository.delete(employee);
    }

    @Transactional(readOnly = true)
    public WorkloadResponse getEmployeeWorkload(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", id));

        List<Allocation> allocations = allocationRepository.findByEmployeeEmployeeId(id).stream()
                .filter(a -> a
                        .getStatus() != com.company.resourceallocation.core.allocation.entity.AllocationStatus.ENDED)
                .toList();
        int allocated = allocations.stream().mapToInt(Allocation::getAllocationPercent).sum();
        int available = 100 - allocated;

        List<WorkloadResponse.AllocationBreakdown> breakdown = allocations.stream()
                .map(a -> new WorkloadResponse.AllocationBreakdown(a.getProject().getProjectCode(),
                        a.getAllocationPercent()))
                .toList();

        return WorkloadResponse.builder()
                .employeeId(employee.getEmployeeId())
                .employeeName(employee.getFullName())
                .allocated(allocated)
                .available(available)
                .allocations(breakdown)
                .build();
    }

    @Transactional
    public void assignSkillsToEmployee(Long employeeId, List<String> skillNames) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", employeeId));

        for (String name : skillNames) {
            if (name == null || name.trim().isEmpty()) {
                continue;
            }
            String trimmedName = name.trim();
            Skill skill = skillRepository.findBySkillNameIgnoreCase(trimmedName)
                    .orElseGet(() -> skillRepository.save(Skill.builder().skillName(trimmedName).build()));
            employee.getSkills().add(skill);
        }
        employeeRepository.save(employee);
    }

    @Transactional(readOnly = true)
    public List<SkillResponse> getEmployeeSkills(Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", employeeId));

        return employee.getSkills().stream()
                .map(skillMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EmployeeSkillSearchResponse> searchEmployeesBySkill(String skillName) {
        List<Employee> employees = employeeRepository.findBySkillName(skillName);
        return employees.stream()
                .map(emp -> {
                    List<Allocation> allocations = allocationRepository.findByEmployeeEmployeeId(emp.getEmployeeId())
                            .stream()
                            .filter(a -> a
                                    .getStatus() != com.company.resourceallocation.core.allocation.entity.AllocationStatus.ENDED)
                            .toList();
                    int allocated = allocations.stream().mapToInt(Allocation::getAllocationPercent).sum();
                    int available = 100 - allocated;
                    return EmployeeSkillSearchResponse.builder()
                            .employeeName(emp.getFullName())
                            .available(available)
                            .build();
                })
                .toList();
    }
}
