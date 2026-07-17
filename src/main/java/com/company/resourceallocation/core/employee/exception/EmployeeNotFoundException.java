package com.company.resourceallocation.core.employee.exception;

import com.company.resourceallocation.exception.ResourceNotFoundException;

public class EmployeeNotFoundException extends ResourceNotFoundException {
    public EmployeeNotFoundException(Long id) {
        super("Employee not found with id: " + id);
    }
}
