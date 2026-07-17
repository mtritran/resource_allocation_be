package com.company.resourceallocation.ai.dto;

import lombok.Builder;
import java.util.List;

@Builder
public record AiRecommendResponse(
    List<RecommendedResource> recommendedResources
) {
    @Builder
    public record RecommendedResource(
        String employee,
        Integer available
    ) {}
}
