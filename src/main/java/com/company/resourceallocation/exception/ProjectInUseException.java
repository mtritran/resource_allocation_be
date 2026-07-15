package com.company.resourceallocation.exception;

public class ProjectInUseException extends RuntimeException {
    public ProjectInUseException(String message) {
        super(message);
    }
}
