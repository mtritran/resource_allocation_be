package com.company.resourceallocation.core.allocation;

import com.company.resourceallocation.core.allocation.dto.AllocationRequest;
import com.company.resourceallocation.core.allocation.dto.AllocationResponse;
import io.swagger.v3.oas.annotations.Operation;
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
    @Operation(summary = "Create Allocation")
    public ResponseEntity<AllocationResponse> createAllocation(@Valid @RequestBody AllocationRequest request) {
        AllocationResponse response = allocationService.createAllocation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Get All Allocations")
    public ResponseEntity<Page<AllocationResponse>> getAllocations(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) Long projectId,
            Pageable pageable) {
        Page<AllocationResponse> response = allocationService.getAllocations(employeeId, projectId, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Allocation by ID")
    public ResponseEntity<AllocationResponse> getAllocationById(@PathVariable Long id) {
        AllocationResponse response = allocationService.getAllocationById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update Allocation")
    public ResponseEntity<AllocationResponse> updateAllocation(
            @PathVariable Long id,
            @Valid @RequestBody AllocationRequest request) {
        AllocationResponse response = allocationService.updateAllocation(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete Allocation")
    public ResponseEntity<Void> deleteAllocation(@PathVariable Long id) {
        allocationService.deleteAllocation(id);
        return ResponseEntity.noContent().build();
    }
}
