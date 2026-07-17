package com.company.resourceallocation.ai.dto;

import lombok.Builder;
import java.util.List;

@Builder
public record AiRiskResponse(
    List<String> risks
) {}
