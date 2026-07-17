package com.company.resourceallocation.core.employee.service;
import com.company.resourceallocation.core.employee.exception.EmployeeInUseException;
import com.company.resourceallocation.core.employee.entity.Employee;
import com.company.resourceallocation.core.employee.repository.EmployeeRepository;
import com.company.resourceallocation.core.employee.mapper.EmployeeMapper;
import com.company.resourceallocation.core.allocation.repository.AllocationRepository;
import com.company.resourceallocation.core.employee.dto.EmployeeRequest;
import com.company.resourceallocation.core.employee.dto.EmployeeResponse;
import com.company.resourceallocation.exception.DuplicateResourceException;
import com.company.resourceallocation.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EmployeeServiceTest {

    @Mock
    EmployeeRepository employeeRepository;

    @Mock
    EmployeeMapper employeeMapper;

    @Mock
    AllocationRepository allocationRepository;

    @InjectMocks
    EmployeeService employeeService;

    EmployeeRequest request;
    Employee employee;
    EmployeeResponse response;

    @BeforeEach
    void setUp() {
        request = EmployeeRequest.builder()
                .employeeCode("EMP001")
                .fullName("Tuan Ho Anh")
                .email("tuanha@company.com")
                .role("Senior Developer")
                .department("FSOFT-Q1")
                .build();

        employee = Employee.builder()
                .employeeId(1L)
                .employeeCode("EMP001")
                .fullName("Tuan Ho Anh")
                .email("tuanha@company.com")
                .role("Senior Developer")
                .department("FSOFT-Q1")
                .build();

        response = EmployeeResponse.builder()
                .employeeId(1L)
                .employeeCode("EMP001")
                .fullName("Tuan Ho Anh")
                .email("tuanha@company.com")
                .role("Senior Developer")
                .department("FSOFT-Q1")
                .build();
    }

    @Test
    void should_createEmployeeSuccessfully_when_validRequest() {
        when(employeeRepository.existsByEmployeeCode(request.getEmployeeCode())).thenReturn(false);
        when(employeeRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(employeeMapper.toEntity(request)).thenReturn(employee);
        when(employeeRepository.save(employee)).thenReturn(employee);
        when(employeeMapper.toResponse(employee)).thenReturn(response);

        EmployeeResponse result = employeeService.createEmployee(request);

        assertNotNull(result);
        assertEquals("EMP001", result.getEmployeeCode());
        verify(employeeRepository).save(employee);
    }

    @Test
    void should_throwDuplicateResourceException_when_duplicateEmployeeCode() {
        when(employeeRepository.existsByEmployeeCode(request.getEmployeeCode())).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> employeeService.createEmployee(request));
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void should_throwDuplicateResourceException_when_duplicateEmail() {
        when(employeeRepository.existsByEmployeeCode(request.getEmployeeCode())).thenReturn(false);
        when(employeeRepository.existsByEmail(request.getEmail())).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> employeeService.createEmployee(request));
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void should_throwResourceNotFoundException_when_getEmployeeByIdNotExists() {
        when(employeeRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> employeeService.getEmployeeById(999L));
    }

    @Test
    void should_throwDuplicateResourceException_when_updateEmailDuplicate() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(employeeRepository.existsByEmployeeCodeAndEmployeeIdNot(request.getEmployeeCode(), 1L)).thenReturn(false);
        when(employeeRepository.existsByEmailAndEmployeeIdNot(request.getEmail(), 1L)).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> employeeService.updateEmployee(1L, request));
    }

    @Test
    void should_throwEmployeeInUseException_when_deleteEmployeeHasActiveAllocations() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(allocationRepository.existsByEmployeeEmployeeId(1L)).thenReturn(true);

        assertThrows(EmployeeInUseException.class, () -> employeeService.deleteEmployee(1L));
        verify(employeeRepository, never()).delete(any(Employee.class));
    }
}
