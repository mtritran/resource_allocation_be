package com.company.resourceallocation.exception;

public class EmployeeInUseException extends RuntimeException {
    public EmployeeInUseException(String message) {
        super(message);
    }
}
