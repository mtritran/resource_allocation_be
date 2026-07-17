package com.company.resourceallocation.core.project.exception;

import com.company.resourceallocation.exception.ResourceNotFoundException;

public class ProjectNotFoundException extends ResourceNotFoundException {
    public ProjectNotFoundException(Long id) {
        super("Project not found with id: " + id);
    }
}
