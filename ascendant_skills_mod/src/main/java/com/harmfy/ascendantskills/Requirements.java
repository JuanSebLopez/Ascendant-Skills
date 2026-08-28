package com.harmfy.ascendantskills;

public final class Requirements {
    private Requirements() {
    }

    public static SkillRequirement forSkill(String skillId) {
        return AscendantConfig.requirement(skillId);
    }
}
