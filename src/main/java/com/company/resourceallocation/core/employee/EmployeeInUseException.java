package com.company.resourceallocation.core.employee;

public class EmployeeInUseException extends RuntimeException {
    public EmployeeInUseException(String message) {
        super(message);
    }
}
