package com.company.resourceallocation.core.project.exception;

public class ProjectInUseException extends RuntimeException {
    public ProjectInUseException(String message) {
        super(message);
    }
}
