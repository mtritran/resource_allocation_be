package com.company.resourceallocation.core.employee;

import com.company.resourceallocation.core.employee.dto.EmployeeRequest;
import com.company.resourceallocation.core.employee.dto.EmployeeResponse;
import com.company.resourceallocation.core.employee.dto.WorkloadResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
@Tag(name = "Employee API", description = "Endpoints for managing employees")
public class EmployeeController {

    private final EmployeeService employeeService;

    @PostMapping
    @Operation(summary = "Create Employee", description = "Create a new employee and validate unique code/email")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Employee created successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "409", description = "Duplicate employee code or email")
    })
    public ResponseEntity<EmployeeResponse> createEmployee(@Parameter(description = "Employee payload") @Valid @RequestBody EmployeeRequest request) {
        EmployeeResponse response = employeeService.createEmployee(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Get All Employees", description = "List employees with optional department/role filters")
    public ResponseEntity<Page<EmployeeResponse>> getEmployees(
            @Parameter(description = "Filter by department") @RequestParam(required = false) String department,
            @Parameter(description = "Filter by role") @RequestParam(required = false) String role,
            Pageable pageable) {
        Page<EmployeeResponse> response = employeeService.getEmployees(department, role, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Employee by ID")
    public ResponseEntity<EmployeeResponse> getEmployeeById(@Parameter(description = "Employee identifier") @PathVariable Long id) {
        EmployeeResponse response = employeeService.getEmployeeById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update Employee")
    public ResponseEntity<EmployeeResponse> updateEmployee(
            @Parameter(description = "Employee identifier") @PathVariable Long id,
            @Parameter(description = "Updated employee payload") @Valid @RequestBody EmployeeRequest request) {
        EmployeeResponse response = employeeService.updateEmployee(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete Employee")
    public ResponseEntity<Void> deleteEmployee(@Parameter(description = "Employee identifier") @PathVariable Long id) {
        employeeService.deleteEmployee(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/workload")
    @Operation(summary = "Get Employee Workload")
    public ResponseEntity<WorkloadResponse> getEmployeeWorkload(@Parameter(description = "Employee identifier") @PathVariable Long id) {
        WorkloadResponse response = employeeService.getEmployeeWorkload(id);
        return ResponseEntity.ok(response);
    }
}
