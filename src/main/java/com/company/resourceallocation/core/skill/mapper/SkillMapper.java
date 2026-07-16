package com.company.resourceallocation.core.skill.mapper;

import com.company.resourceallocation.core.skill.entity.Skill;
import com.company.resourceallocation.core.skill.dto.SkillResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SkillMapper {
    SkillResponse toResponse(Skill skill);
}
