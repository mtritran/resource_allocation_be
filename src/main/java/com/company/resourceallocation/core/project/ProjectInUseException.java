package com.company.resourceallocation.core.project;

public class ProjectInUseException extends RuntimeException {
    public ProjectInUseException(String message) {
        super(message);
    }
}
