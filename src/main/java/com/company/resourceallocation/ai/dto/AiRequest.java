package com.company.resourceallocation.ai.dto;

import lombok.Builder;

@Builder
public record AiRequest(
    String query
) {}
