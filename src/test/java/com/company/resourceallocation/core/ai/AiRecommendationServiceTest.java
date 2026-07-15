package com.company.resourceallocation.core.ai;

import com.company.resourceallocation.core.ai.dto.AiRecommendResponse;
import com.company.resourceallocation.core.ai.dto.AiRiskResponse;
import com.company.resourceallocation.core.report.ReportService;
import com.company.resourceallocation.core.report.dto.AvailableResponse;
import com.company.resourceallocation.core.report.dto.OverloadedResponse;
import com.company.resourceallocation.core.report.dto.UtilizationResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AiRecommendationServiceTest {

    @Mock
    ReportService reportService;

    @Mock
    GeminiClient geminiClient;

    @InjectMocks
    AiRecommendationService aiRecommendationService;

    @Test
    void should_returnRecommendedResources_when_geminiRespondsWithValidJson() {
        // Arrange: dữ liệu thật từ database
        when(reportService.getAvailableReport(1)).thenReturn(List.of(
                new AvailableResponse(1L, "Nguyen Van A", 60),
                new AvailableResponse(2L, "Tran Thi B", 20)
        ));

        // Gemini trả về JSON chuẩn
        when(geminiClient.call(anyString())).thenReturn("""
                {
                  "recommendedResources": [
                    { "employee": "Nguyen Van A", "available": 60 }
                  ]
                }
                """);

        // Act
        AiRecommendResponse result = aiRecommendationService.getRecommendations("Tìm Java Developer còn tối thiểu 50% available");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getRecommendedResources().size());
        assertEquals("Nguyen Van A", result.getRecommendedResources().get(0).getEmployee());
        assertEquals(60, result.getRecommendedResources().get(0).getAvailable());
    }

    @Test
    void should_fallbackToRealData_when_geminiResponseIsNotValidJson() {
        when(reportService.getAvailableReport(1)).thenReturn(List.of(
                new AvailableResponse(1L, "Nguyen Van A", 60),
                new AvailableResponse(2L, "Tran Thi B", 20)
        ));

        // Gemini trả về text bình thường, không phải JSON
        when(geminiClient.call(anyString())).thenReturn("Xin chào! Tôi có thể giúp gì cho bạn?");

        // Khi parse lỗi → fallback về raw data từ database
        AiRecommendResponse result = aiRecommendationService.getRecommendations("query test");

        assertNotNull(result);
        assertEquals(2, result.getRecommendedResources().size()); // fallback trả tất cả available
    }

    @Test
    void should_returnRisks_when_geminiRespondsWithValidJson() {
        when(reportService.getUtilizationReport()).thenReturn(List.of(
                new UtilizationResponse(1L, "Nguyen Van A", 100),
                new UtilizationResponse(2L, "Tran Thi B", 80),
                new UtilizationResponse(3L, "Le Van C", 40)
        ));
        when(reportService.getOverloadedReport()).thenReturn(List.of(
                new OverloadedResponse(1L, "Nguyen Van A", 100)
        ));
        when(reportService.getAvailableReport(50)).thenReturn(List.of(
                new AvailableResponse(3L, "Le Van C", 60)
        ));

        when(geminiClient.call(anyString())).thenReturn("""
                {
                  "risks": [
                    "Team đang sử dụng 73.3% capacity.",
                    "Có 1 nhân viên bị overloaded.",
                    "Chỉ còn 1 resource available trên 50%."
                  ]
                }
                """);

        AiRiskResponse result = aiRecommendationService.detectRisks("Sprint tới cần thêm 2 Java Developer");

        assertNotNull(result);
        assertEquals(3, result.getRisks().size());
        assertTrue(result.getRisks().get(0).contains("capacity"));
    }

    @Test
    void should_fallbackToRealData_when_geminiRiskResponseIsNotValidJson() {
        when(reportService.getUtilizationReport()).thenReturn(List.of(
                new UtilizationResponse(1L, "A", 95),
                new UtilizationResponse(2L, "B", 80)
        ));
        when(reportService.getOverloadedReport()).thenReturn(List.of(
                new OverloadedResponse(1L, "A", 95)
        ));
        when(reportService.getAvailableReport(50)).thenReturn(List.of());

        when(geminiClient.call(anyString())).thenReturn("Lỗi API.");

        AiRiskResponse result = aiRecommendationService.detectRisks("query test");

        assertNotNull(result);
        assertFalse(result.getRisks().isEmpty()); // fallback tạo risks từ data thật
        assertTrue(result.getRisks().stream().anyMatch(r -> r.contains("87.5") || r.contains("overloaded") || r.contains("available")));
    }
}
