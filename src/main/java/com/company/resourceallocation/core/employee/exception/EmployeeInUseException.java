package com.company.resourceallocation.core.employee.exception;

public class EmployeeInUseException extends RuntimeException {
    public EmployeeInUseException(String message) {
        super(message);
    }
}
