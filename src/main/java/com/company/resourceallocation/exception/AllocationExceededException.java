package com.company.resourceallocation.exception;

public class AllocationExceededException extends RuntimeException {
    public AllocationExceededException(String message) {
        super(message);
    }
}
