package com.company.resourceallocation.core.skill.dto;

import lombok.Builder;

@Builder
public record SkillResponse(
    Long skillId,
    String skillName
) {}
