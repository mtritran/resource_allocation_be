package com.company.resourceallocation.core.allocation.exception;

public class AllocationExceededException extends RuntimeException {
    public AllocationExceededException(String message) {
        super(message);
    }
}
