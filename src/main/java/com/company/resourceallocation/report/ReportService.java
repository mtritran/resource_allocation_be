package com.company.resourceallocation.report;

import com.company.resourceallocation.core.allocation.AllocationRepository;
import com.company.resourceallocation.report.dto.AvailableResponse;
import com.company.resourceallocation.report.dto.OverloadedResponse;
import com.company.resourceallocation.report.dto.UtilizationResponse;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.AccessLevel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ReportService {

    AllocationRepository allocationRepository;

    @Transactional(readOnly = true)
    public List<UtilizationResponse> getUtilizationReport() {
        List<Object[]> results = allocationRepository.getUtilizationReport();
        return results.stream().map(row -> {
            Long empId = ((Number) row[0]).longValue();
            String name = (String) row[1];
            Integer total = ((Number) row[2]).intValue();
            return new UtilizationResponse(empId, name, total);
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<AvailableResponse> getAvailableReport(int minAvailable) {
        List<Object[]> results = allocationRepository.getAvailableReport(minAvailable);
        return results.stream().map(row -> {
            Long empId = ((Number) row[0]).longValue();
            String name = (String) row[1];
            Integer available = ((Number) row[2]).intValue();
            return new AvailableResponse(empId, name, available);
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<OverloadedResponse> getOverloadedReport() {
        List<Object[]> results = allocationRepository.getOverloadedReport();
        return results.stream().map(row -> {
            Long empId = ((Number) row[0]).longValue();
            String name = (String) row[1];
            Integer total = ((Number) row[2]).intValue();
            return new OverloadedResponse(empId, name, total);
        }).toList();
    }
}
