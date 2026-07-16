package com.company.resourceallocation.core.allocation.controller;
import com.company.resourceallocation.core.allocation.service.AllocationService;
import com.company.resourceallocation.core.allocation.dto.AllocationRequest;
import com.company.resourceallocation.core.allocation.dto.AllocationResponse;


import com.company.resourceallocation.core.allocation.dto.AllocationRequest;
import com.company.resourceallocation.core.allocation.dto.AllocationResponse;
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
@RequestMapping("/api/v1/allocations")
@RequiredArgsConstructor
@Tag(name = "Allocation API", description = "Endpoints for managing allocations")
public class AllocationController {

    private final AllocationService allocationService;

    @PostMapping
    @Operation(summary = "Create Allocation", description = "Create a new allocation while enforcing 100% capacity rules")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Allocation created successfully"),
            @ApiResponse(responseCode = "400", description = "Validation or business-rule failure"),
            @ApiResponse(responseCode = "404", description = "Employee or project not found")
    })
    public ResponseEntity<AllocationResponse> createAllocation(@Parameter(description = "Allocation payload") @Valid @RequestBody AllocationRequest request) {
        AllocationResponse response = allocationService.createAllocation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Get All Allocations", description = "List allocations filtered by employee or project")
    public ResponseEntity<Page<AllocationResponse>> getAllocations(
            @Parameter(description = "Filter by employee identifier") @RequestParam(required = false) Long employeeId,
            @Parameter(description = "Filter by project identifier") @RequestParam(required = false) Long projectId,
            Pageable pageable) {
        Page<AllocationResponse> response = allocationService.getAllocations(employeeId, projectId, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Allocation by ID")
    public ResponseEntity<AllocationResponse> getAllocationById(@Parameter(description = "Allocation identifier") @PathVariable Long id) {
        AllocationResponse response = allocationService.getAllocationById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update Allocation")
    public ResponseEntity<AllocationResponse> updateAllocation(
            @Parameter(description = "Allocation identifier") @PathVariable Long id,
            @Parameter(description = "Updated allocation payload") @Valid @RequestBody AllocationRequest request) {
        AllocationResponse response = allocationService.updateAllocation(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete Allocation")
    public ResponseEntity<Void> deleteAllocation(@Parameter(description = "Allocation identifier") @PathVariable Long id) {
        allocationService.deleteAllocation(id);
        return ResponseEntity.noContent().build();
    }
}
