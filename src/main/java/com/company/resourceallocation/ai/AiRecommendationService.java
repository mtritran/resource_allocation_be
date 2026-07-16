package com.company.resourceallocation.ai;

import com.company.resourceallocation.ai.dto.AiRecommendResponse;
import com.company.resourceallocation.ai.dto.AiRiskResponse;
import com.company.resourceallocation.core.employee.repository.EmployeeRepository;
import com.company.resourceallocation.report.ReportService;
import com.company.resourceallocation.report.dto.AvailableResponse;
import com.company.resourceallocation.report.dto.OverloadedResponse;
import com.company.resourceallocation.report.dto.UtilizationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.experimental.FieldDefaults;
import lombok.AccessLevel;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AiRecommendationService {

    ReportService reportService;
    GeminiClient geminiClient;
    EmployeeRepository employeeRepository;

    /**
     * AI Resource Recommendation — endpoint 5.1
     * Flow: lấy available report thật + roles → build prompt linh hoạt cho AI lọc → gọi Gemini format
     */
    public AiRecommendResponse getRecommendations(String query) {
        if (query == null || query.trim().isEmpty() || query.trim().equalsIgnoreCase("string")) {
            return AiRecommendResponse.builder().recommendedResources(List.of()).build();
        }

        // 1. Lấy dữ liệu thật từ database (minAvailable = 1 để lấy tất cả còn available)
        List<AvailableResponse> availableList = reportService.getAvailableReport(1);

        // 2. Fetch full employee details (name, role, available %) to build context for Gemini
        String realDataContext = availableList.stream()
                .map(a -> {
                    String role = employeeRepository.findById(a.getEmployeeId())
                            .map(emp -> emp.getRole())
                            .orElse("N/A");
                    return "- %s (Role: %s): available=%d%%".formatted(a.getEmployeeName(), role, a.getAvailable());
                })
                .collect(Collectors.joining("\n"));

        // 3. Build prompt — AI có đầy đủ tên & role & available% thực tế để tự lọc linh hoạt
        String prompt = """
                Bạn là AI hỗ trợ quản lý phân bổ nhân sự. Dưới đây là danh sách nhân viên còn khả năng làm việc (available capacity) LẤY TRỰC TIẾP TỪ DATABASE — bạn KHÔNG ĐƯỢC tự bịa thêm hoặc thay đổi bất kỳ con số nào:

                %s

                Yêu cầu người dùng: "%s"

                Hãy thực hiện các yêu cầu sau:
                1. Nếu yêu cầu của người dùng là chào hỏi, nói nhảm, test từ khóa mặc định (như "string"), hoặc không liên quan đến việc tìm kiếm/đề xuất nhân sự từ danh sách trên, hãy trả về danh sách rỗng:
                   {
                     "recommendedResources": []
                   }
                2. Nếu yêu cầu hợp lệ, hãy lọc từ danh sách thực tế ở trên những nhân sự thỏa mãn yêu cầu của người dùng (về Role/Chức vụ và phần trăm Available). Trả về theo định dạng JSON sau:
                   {
                     "recommendedResources": [
                       { "employee": "Tên nhân viên thực tế", "available": số_phần_trăm_available_thực_tế }
                     ]
                   }
                Chỉ trả về JSON, không thêm bất kỳ văn bản hay giải thích nào khác.
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
            log.warn("Gemini call or parse failed, falling back to offline parsing. Error: {}", e.getMessage());
            
            // Fallback offline: tự lọc bằng regex khi Gemini bị lỗi/chặn hạn mức
            int minAvailable = extractMinAvailable(query);
            String parsedRole = extractRole(query);
            String queryClean = query.trim().toLowerCase();

            // Nếu câu hỏi không chứa thông tin về role hay threshold trong chế độ fallback → trả về trống
            if (parsedRole == null && !queryClean.matches(".*\\d+.*") && !queryClean.contains("rảnh") && !queryClean.contains("available")) {
                return AiRecommendResponse.builder().recommendedResources(List.of()).build();
            }

            List<AiRecommendResponse.RecommendedResource> resources = availableList.stream()
                    .filter(a -> employeeRepository.findById(a.getEmployeeId())
                            .map(emp -> {
                                if (parsedRole != null) {
                                    return emp.getRole().toLowerCase().contains(parsedRole.toLowerCase());
                                }
                                return emp.getFullName().toLowerCase().contains(queryClean) ||
                                       emp.getRole().toLowerCase().contains(queryClean);
                            }).orElse(false))
                    .filter(a -> a.getAvailable() >= minAvailable)
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
        if (query == null || query.trim().isEmpty() || query.trim().equalsIgnoreCase("string")) {
            return AiRiskResponse.builder().risks(List.of()).build();
        }

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

                Hãy thực hiện các yêu cầu sau:
                1. Nếu yêu cầu của người dùng là chào hỏi, nói nhảm, hoặc test từ khóa mặc định (như "string"), hãy trả về danh sách rỗng:
                   {
                     "risks": []
                   }
                2. Nếu yêu cầu là phân tích rủi ro hợp lệ (ví dụ: "Sprint tới cần thêm nhân sự", "check rủi ro", "capacity hiện tại thế nào"), hãy trả về danh sách các rủi ro theo định dạng JSON sau:
                   {
                     "risks": [
                       "Mô tả rủi ro 1 với số liệu thực tế",
                       "Mô tả rủi ro 2 với số liệu thực tế"
                     ]
                   }
                Chỉ trả về JSON, không thêm bất kỳ giải thích nào khác. Số liệu trong risks phải khớp với data thực tế ở trên.
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
            
            // Fallback offline: tự sinh phân tích dựa trên dữ liệu thô
            String queryClean = query.trim().toLowerCase();
            if (!queryClean.contains("sprint") && !queryClean.contains("risk") && !queryClean.contains("rủi ro") && !queryClean.contains("cần") && !queryClean.contains("thêm")) {
                return AiRiskResponse.builder().risks(List.of()).build();
            }
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

    private int extractMinAvailable(String query) {
        if (query == null) return 1;
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+)\\s*%");
        java.util.regex.Matcher matcher = pattern.matcher(query);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        pattern = java.util.regex.Pattern.compile("tối thiểu\\s+(\\d+)");
        matcher = pattern.matcher(query.toLowerCase());
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        return 1;
    }

    private String extractRole(String query) {
        if (query == null) return null;
        String q = query.toLowerCase();
        if (q.contains("java")) return "Java";
        if (q.contains("react")) return "React";
        if (q.contains("devops")) return "DevOps";
        if (q.contains("qa") || q.contains("tester")) return "QA";
        if (q.contains("pm") || q.contains("project manager")) return "Project Manager";
        return null;
    }
}
