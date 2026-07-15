package com.company.resourceallocation.core.employee;

import com.company.resourceallocation.core.employee.dto.EmployeeRequest;
import com.company.resourceallocation.core.employee.dto.EmployeeResponse;
import com.company.resourceallocation.core.allocation.AllocationRepository;
import com.company.resourceallocation.exception.DuplicateResourceException;
import com.company.resourceallocation.exception.EmployeeInUseException;
import com.company.resourceallocation.exception.ResourceNotFoundException;
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

    @Transactional
    public EmployeeResponse createEmployee(EmployeeRequest request) {
        if (employeeRepository.existsByEmployeeCode(request.getEmployeeCode())) {
            throw new DuplicateResourceException("Employee code " + request.getEmployeeCode() + " already exists");
        }
        if (employeeRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email " + request.getEmail() + " already exists");
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

        if (employeeRepository.existsByEmployeeCodeAndEmployeeIdNot(request.getEmployeeCode(), id)) {
            throw new DuplicateResourceException("Employee code " + request.getEmployeeCode() + " already exists");
        }
        if (employeeRepository.existsByEmailAndEmployeeIdNot(request.getEmail(), id)) {
            throw new DuplicateResourceException("Email " + request.getEmail() + " already exists");
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
}
