package com.company.resourceallocation.report;

import com.company.resourceallocation.report.dto.AvailableResponse;
import com.company.resourceallocation.report.dto.OverloadedResponse;
import com.company.resourceallocation.report.dto.UtilizationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Tag(name = "Report API", description = "Endpoints for resource utilization and availability reporting")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/utilization")
    @Operation(summary = "Get Employee Utilization Report")
    public ResponseEntity<List<UtilizationResponse>> getUtilizationReport() {
        return ResponseEntity.ok(reportService.getUtilizationReport());
    }

    @GetMapping("/available")
    @Operation(summary = "Get Available Resource Report")
    public ResponseEntity<List<AvailableResponse>> getAvailableReport(
            @RequestParam(required = false) Integer minAvailable) {
        int threshold = (minAvailable != null) ? minAvailable : 1;
        return ResponseEntity.ok(reportService.getAvailableReport(threshold));
    }

    @GetMapping("/overloaded")
    @Operation(summary = "Get Overloaded Employee Report")
    public ResponseEntity<List<OverloadedResponse>> getOverloadedReport() {
        return ResponseEntity.ok(reportService.getOverloadedReport());
    }
}
