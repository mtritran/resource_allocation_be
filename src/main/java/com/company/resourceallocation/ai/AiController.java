package com.company.resourceallocation.ai;

import com.company.resourceallocation.ai.dto.AiRecommendResponse;
import com.company.resourceallocation.ai.dto.AiRequest;
import com.company.resourceallocation.ai.dto.AiRiskResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
@Tag(name = "AI API", description = "AI-powered resource recommendation and risk detection")
public class AiController {

    private final AiRecommendationService aiRecommendationService;

    @PostMapping("/recommend")
    @Operation(summary = "AI Resource Recommendation — trả danh sách nhân viên available phù hợp query, số liệu lấy từ database thật")
    public ResponseEntity<AiRecommendResponse> recommend(@Valid @RequestBody AiRequest request) {
        AiRecommendResponse response = aiRecommendationService.getRecommendations(request.query());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/risk-detection")
    @Operation(summary = "AI Risk Detection — phân tích rủi ro nhân sự dựa trên dữ liệu utilization thật")
    public ResponseEntity<AiRiskResponse> detectRisk(@Valid @RequestBody AiRequest request) {
        AiRiskResponse response = aiRecommendationService.detectRisks(request.query());
        return ResponseEntity.ok(response);
    }
}
