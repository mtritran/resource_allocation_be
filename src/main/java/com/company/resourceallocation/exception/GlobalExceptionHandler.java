package com.company.resourceallocation.exception;

import com.company.resourceallocation.core.employee.exception.EmployeeInUseException;
import com.company.resourceallocation.core.employee.exception.EmployeeNotFoundException;
import com.company.resourceallocation.core.project.exception.ProjectInUseException;
import com.company.resourceallocation.core.project.exception.ProjectNotFoundException;
import com.company.resourceallocation.core.project.exception.InvalidProjectStatusException;
import com.company.resourceallocation.core.allocation.exception.AllocationExceededException;
import com.company.resourceallocation.core.allocation.exception.InvalidAllocationPercentageException;
import com.company.resourceallocation.core.allocation.exception.InvalidAllocationStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({EmployeeNotFoundException.class, ProjectNotFoundException.class, ResourceNotFoundException.class})
    public ProblemDetail handleNotFound(RuntimeException ex) {
        String code = "RESOURCE_NOT_FOUND";
        if (ex instanceof EmployeeNotFoundException) {
            code = "EMPLOYEE_NOT_FOUND";
        } else if (ex instanceof ProjectNotFoundException) {
            code = "PROJECT_NOT_FOUND";
        }
        return buildProblemDetail(HttpStatus.NOT_FOUND, ex.getMessage(), code);
    }

    @ExceptionHandler({DuplicateResourceException.class, EmployeeInUseException.class, ProjectInUseException.class})
    public ProblemDetail handleConflict(RuntimeException ex) {
        String code = "RESOURCE_CONFLICT";
        if (ex instanceof DuplicateResourceException) {
            code = "DUPLICATE_RESOURCE";
        } else if (ex instanceof EmployeeInUseException) {
            code = "EMPLOYEE_IN_USE";
        } else if (ex instanceof ProjectInUseException) {
            code = "PROJECT_IN_USE";
        }
        return buildProblemDetail(HttpStatus.CONFLICT, ex.getMessage(), code);
    }

    @ExceptionHandler({
            AllocationExceededException.class,
            InvalidProjectStatusException.class,
            InvalidAllocationPercentageException.class,
            InvalidAllocationStatusException.class
    })
    public ProblemDetail handleBadRequest(RuntimeException ex) {
        String code = "BAD_REQUEST";
        if (ex instanceof AllocationExceededException) {
            code = "ALLOCATION_EXCEEDED";
        } else if (ex instanceof InvalidProjectStatusException) {
            code = "INVALID_PROJECT_STATUS";
        } else if (ex instanceof InvalidAllocationPercentageException) {
            code = "INVALID_ALLOCATION_PERCENTAGE";
        } else if (ex instanceof InvalidAllocationStatusException) {
            code = "INVALID_ALLOCATION_STATUS";
        }
        return buildProblemDetail(HttpStatus.BAD_REQUEST, ex.getMessage(), code);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return buildProblemDetail(HttpStatus.BAD_REQUEST, message, "VALIDATION_FAILED");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        return buildProblemDetail(HttpStatus.BAD_REQUEST, ex.getMessage(), "INVALID_ARGUMENT");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleMessageNotReadable(HttpMessageNotReadableException ex) {
        return buildProblemDetail(HttpStatus.BAD_REQUEST, "Malformed JSON request or invalid values: " + ex.getMessage(), "MALFORMED_REQUEST");
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneral(Exception ex) {
        return buildProblemDetail(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), "INTERNAL_SERVER_ERROR");
    }

    private ProblemDetail buildProblemDetail(HttpStatus status, String detail, String code) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setTitle(code);
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("errorCode", code);
        problemDetail.setProperty("message", detail);
        return problemDetail;
    }
}
