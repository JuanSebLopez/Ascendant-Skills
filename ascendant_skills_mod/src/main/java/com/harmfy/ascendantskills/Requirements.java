package com.harmfy.ascendantskills;

import java.util.Map;

public final class Requirements {
    private static final Map<String, SkillRequirement> REQUIREMENTS = Map.ofEntries(
            Map.entry("inicio", SkillRequirement.levels(1)),
            Map.entry("combate", SkillRequirement.levels(1)),
            Map.entry("cazador", SkillRequirement.levels(1)),
            Map.entry("combatiente", SkillRequirement.levels(1)),
            Map.entry("bastion", SkillRequirement.levels(1)),

            Map.entry("punteria", SkillRequirement.levels(1)),
            Map.entry("ojo_certero", SkillRequirement.levels(1)),
            Map.entry("tiro_rapido", SkillRequirement.levels(1)),
            Map.entry("francotirador", SkillRequirement.levelsAndBoss(1, "cataclysm:ignis")),
            Map.entry("aljaba_ligera", SkillRequirement.levelsAndBoss(1, "bosses_of_mass_destruction:void_blossom")),
            Map.entry("perforador", SkillRequirement.levels(1)),
            Map.entry("escaramuzador", SkillRequirement.levels(1)),
            Map.entry("deadeye", SkillRequirement.levelsAndBoss(1, "cataclysm:scylla")),
            Map.entry("barrage", SkillRequirement.levelsAndBoss(1, "cataclysm:the_leviathan")),
            Map.entry("maestro_tirador", SkillRequirement.levelsAndBoss(1, "minecraft:ender_dragon")),

            Map.entry("vanguardia", SkillRequirement.levels(1)),
            Map.entry("coraza_de_acero", SkillRequirement.levels(1)),
            Map.entry("danzante_de_acero", SkillRequirement.levels(1)),
            Map.entry("verdugo", SkillRequirement.levels(1)),
            Map.entry("inamovible", SkillRequirement.levelsAndBoss(1, "cataclysm:netherite_monstrosity")),
            Map.entry("veterano_de_guerra", SkillRequirement.levels(1)),
            Map.entry("berserker", SkillRequirement.levels(1)),
            Map.entry("juggernaut", SkillRequirement.levels(1)),
            Map.entry("conquistador", SkillRequirement.levelsAndBoss(1, "cataclysm:maledictus")),
            Map.entry("senor_de_la_guerra", SkillRequirement.levelsAndBoss(1, "minecraft:ender_dragon")),

            Map.entry("muralla", SkillRequirement.levelsAndBoss(1, "mowziesmobs:ferrous_wroughtnaut")),
            Map.entry("fortaleza", SkillRequirement.levels(1)),
            Map.entry("guardaespaldas", SkillRequirement.levels(1)),
            Map.entry("coloso", SkillRequirement.levelsAndBoss(1, "cataclysm:netherite_monstrosity")),
            Map.entry("provocador", SkillRequirement.levelsAndBoss(1, "bosses_of_mass_destruction:void_blossom")),
            Map.entry("regenerador", SkillRequirement.levels(1)),
            Map.entry("invencible", SkillRequirement.levels(1)),
            Map.entry("bastion_absoluto", SkillRequirement.levelsAndBoss(1, "cataclysm:scylla")),
            Map.entry("titan", SkillRequirement.levelsAndBoss(1, "minecraft:ender_dragon"))
    );

    private Requirements() {
    }

    public static SkillRequirement forSkill(String skillId) {
        return REQUIREMENTS.getOrDefault(skillId, SkillRequirement.levels(1));
    }
}
