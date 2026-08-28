package com.harmfy.ascendantskills;

import java.util.Optional;

public record SkillRequirement(int levels, Optional<String> bossId) {
    public static SkillRequirement levels(int levels) {
        return new SkillRequirement(levels, Optional.empty());
    }

    public static SkillRequirement levelsAndBoss(int levels, String bossId) {
        return new SkillRequirement(levels, Optional.of(bossId));
    }
}
