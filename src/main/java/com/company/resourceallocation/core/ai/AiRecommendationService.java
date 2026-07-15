package com.company.resourceallocation.core.ai;

import com.company.resourceallocation.core.ai.dto.AiRecommendResponse;
import com.company.resourceallocation.core.ai.dto.AiRiskResponse;
import com.company.resourceallocation.core.report.ReportService;
import com.company.resourceallocation.core.report.dto.AvailableResponse;
import com.company.resourceallocation.core.report.dto.OverloadedResponse;
import com.company.resourceallocation.core.report.dto.UtilizationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.experimental.FieldDefaults;
import lombok.AccessLevel;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AiRecommendationService {

    ReportService reportService;
    GeminiClient geminiClient;

    /**
     * AI Resource Recommendation — endpoint 5.1
     * Flow: lấy available report thật → build prompt có số thật → gọi Gemini format → trả về danh sách
     */
    public AiRecommendResponse getRecommendations(String query) {
        // 1. Lấy dữ liệu thật từ database (minAvailable = 1 để lấy tất cả còn available)
        List<AvailableResponse> availableList = reportService.getAvailableReport(1);

        // 2. Serialize thành context cho prompt
        String realDataContext = availableList.stream()
                .map(a -> "- %s: available=%d%%".formatted(a.getEmployeeName(), a.getAvailable()))
                .collect(Collectors.joining("\n"));

        // 3. Build prompt — số liệu 100% từ database, AI chỉ format câu trả lời
        String prompt = """
                Bạn là AI hỗ trợ quản lý phân bổ nhân sự. Dưới đây là danh sách nhân viên còn khả năng làm việc (available capacity) LẤY TRỰC TIẾP TỪ DATABASE — bạn KHÔNG ĐƯỢC tự bịa thêm hoặc thay đổi bất kỳ con số nào:

                %s

                Yêu cầu người dùng: "%s"

                Hãy lọc từ danh sách THỰC TẾ ở trên những nhân viên phù hợp với yêu cầu và trả lời ngắn gọn, chỉ liệt kê tên nhân viên và %% available. Trả về theo đúng định dạng JSON:
                {
                  "recommendedResources": [
                    { "employee": "Tên nhân viên", "available": số_thực_tế }
                  ]
                }
                Chỉ trả về JSON, không thêm bất kỳ giải thích nào khác.
                """.formatted(realDataContext, query);

        try {
            // 4. Gọi Gemini
            String rawResponse = geminiClient.call(prompt);
            log.debug("Gemini recommend raw response: {}", rawResponse);

            // Trích JSON từ response (loại bỏ markdown code block nếu có)
            String json = extractJson(rawResponse);
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(json, AiRecommendResponse.class);
        } catch (Exception e) {
            log.warn("Gemini call or parse failed, falling back to raw data. Error: {}", e.getMessage());
            // Fallback: trả thẳng data từ database, không qua AI format
            List<AiRecommendResponse.RecommendedResource> resources = availableList.stream()
                    .map(a -> new AiRecommendResponse.RecommendedResource(a.getEmployeeName(), a.getAvailable()))
                    .toList();
            return AiRecommendResponse.builder().recommendedResources(resources).build();
        }
    }

    /**
     * AI Risk Detection — endpoint 5.2
     * Flow: lấy utilization + overloaded report thật → build prompt có số thật → gọi Gemini → trả risks
     */
    public AiRiskResponse detectRisks(String query) {
        // 1. Lấy dữ liệu thật
        List<UtilizationResponse> utilizationList = reportService.getUtilizationReport();
        List<OverloadedResponse> overloadedList = reportService.getOverloadedReport();
        List<AvailableResponse> highAvailableList = reportService.getAvailableReport(50);

        // 2. Tính toán metrics thực tế từ database
        double avgUtilization = utilizationList.isEmpty() ? 0 :
                utilizationList.stream().mapToInt(UtilizationResponse::getTotalAllocation).average().orElse(0);
        int totalEmployees = utilizationList.size();
        int overloadedCount = overloadedList.size();
        int highAvailableCount = highAvailableList.size();

        String utilizationContext = utilizationList.stream()
                .map(u -> "  - %s: %d%%".formatted(u.getEmployeeName(), u.getTotalAllocation()))
                .collect(Collectors.joining("\n"));

        String overloadedContext = overloadedList.isEmpty() ? "  (không có)" :
                overloadedList.stream()
                        .map(o -> "  - %s: %d%%".formatted(o.getEmployeeName(), o.getTotalAllocation()))
                        .collect(Collectors.joining("\n"));

        // 3. Build prompt với số liệu thật
        String prompt = """
                Bạn là AI phân tích rủi ro nhân sự. Dưới đây là dữ liệu THỰC TẾ từ hệ thống — KHÔNG ĐƯỢC tự bịa số:

                TỔNG QUAN:
                - Tổng số nhân viên: %d
                - Utilization trung bình: %.1f%%
                - Số nhân viên bị overloaded (>90%%): %d
                - Số nhân viên còn >=50%% available: %d

                DANH SÁCH UTILIZATION TỪNG NGƯỜI:
                %s

                DANH SÁCH OVERLOADED:
                %s

                Yêu cầu người dùng: "%s"

                Dựa vào dữ liệu THỰC TẾ trên, hãy phân tích rủi ro và trả về JSON:
                {
                  "risks": [
                    "Mô tả rủi ro 1 với số liệu thực tế",
                    "Mô tả rủi ro 2 với số liệu thực tế"
                  ]
                }
                Chỉ trả về JSON, không thêm giải thích. Số liệu trong risks phải khớp với data thực tế ở trên.
                """.formatted(totalEmployees, avgUtilization, overloadedCount, highAvailableCount,
                utilizationContext, overloadedContext, query);

        try {
            // 4. Gọi Gemini
            String rawResponse = geminiClient.call(prompt);
            log.debug("Gemini risk raw response: {}", rawResponse);

            String json = extractJson(rawResponse);
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(json, AiRiskResponse.class);
        } catch (Exception e) {
            log.warn("Gemini call or parse failed, falling back to raw risks. Error: {}", e.getMessage());
            // Fallback: tạo risks từ data thật
            List<String> risks = List.of(
                    "Utilization trung bình của team là %.1f%%.".formatted(avgUtilization),
                    "Có %d nhân viên overloaded (>90%%).".formatted(overloadedCount),
                    "Chỉ có %d nhân viên còn >=50%% available.".formatted(highAvailableCount)
            );
            return AiRiskResponse.builder().risks(risks).build();
        }
    }

    /**
     * Trích phần JSON thuần túy từ response Gemini (loại bỏ markdown code block ```json ... ```)
     */
    private String extractJson(String raw) {
        String trimmed = raw.trim();
        // Loại bỏ ```json ... ``` hoặc ``` ... ```
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            int lastBacktick = trimmed.lastIndexOf("```");
            if (firstNewline > 0 && lastBacktick > firstNewline) {
                trimmed = trimmed.substring(firstNewline + 1, lastBacktick).trim();
            }
        }
        return trimmed;
    }
}
