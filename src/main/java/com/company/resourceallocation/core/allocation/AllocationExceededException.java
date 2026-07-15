package com.company.resourceallocation.core.allocation;

public class AllocationExceededException extends RuntimeException {
    public AllocationExceededException(String message) {
        super(message);
    }
}
