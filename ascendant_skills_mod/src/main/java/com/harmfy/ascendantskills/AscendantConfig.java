package com.harmfy.ascendantskills;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.annotations.SerializedName;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public final class AscendantConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = FMLPaths.CONFIGDIR.get().resolve(AscendantSkills.MOD_ID);
    private static final Path REQUIREMENTS_PATH = CONFIG_DIR.resolve("requirements.json");
    private static final Path GAMEPLAY_PATH = CONFIG_DIR.resolve("gameplay.json");
    private static Map<String, RequirementEntry> requirements = defaultRequirements();
    private static GameplayFile gameplay = defaultGameplay();

    private AscendantConfig() {
    }

    public static void loadOrCreate() {
        try {
            Files.createDirectories(CONFIG_DIR);
            requirements = loadOrWriteDefaultRequirements();
            gameplay = sanitize(loadOrWriteDefault(GAMEPLAY_PATH, GameplayFile.class, defaultGameplay()), defaultGameplay());
            AscendantSkills.LOGGER.info("Loaded Ascendant Skills config from {}", CONFIG_DIR);
        } catch (IOException | RuntimeException ex) {
            AscendantSkills.LOGGER.error("Failed to load Ascendant Skills config. Using in-memory defaults.", ex);
            requirements = defaultRequirements();
            gameplay = defaultGameplay();
        }
    }

    public static SkillRequirement requirement(String skillId) {
        RequirementEntry entry = requirements.get(skillId);
        if (entry == null) {
            return SkillRequirement.levels(1);
        }
        return new SkillRequirement(Math.max(0, entry.levels), Optional.ofNullable(blankToNull(entry.boss)));
    }

    public static Set<String> bossIds() {
        return requirements.values().stream()
                .map(entry -> blankToNull(entry.boss))
                .filter(boss -> boss != null)
                .collect(Collectors.toUnmodifiableSet());
    }

    public static Set<String> skillIds() {
        return Set.copyOf(requirements.keySet());
    }

    public static Set<String> perkIds() {
        return requirements.keySet().stream()
                .map(skillId -> AscendantSkills.MOD_ID + ":" + skillId)
                .collect(Collectors.toUnmodifiableSet());
    }

    public static boolean disableVanillaJumpCriticals() {
        return gameplay.disableVanillaJumpCriticals;
    }

    public static boolean disableVanillaFullyChargedBowCriticals() {
        return gameplay.disableVanillaFullyChargedBowCriticals;
    }

    public static long bossCreditWindowTicks() {
        return Math.max(1, gameplay.bossCreditWindowSeconds) * 20L;
    }

    public static int maxSoloBossCreditPlayers() {
        return Math.max(1, gameplay.maxSoloBossCreditPlayers);
    }

    public static int minPartySize() {
        return Math.max(1, gameplay.minPartySize);
    }

    public static int maxPartySize() {
        return Math.max(minPartySize(), gameplay.maxPartySize);
    }

    public static double globalResistanceSoftCap() {
        return clamp(orDefault(gameplay.globalResistanceSoftCap, 0.20D), 0.0D, 0.95D);
    }

    public static double globalResistanceOverflowMultiplier() {
        return clamp(orDefault(gameplay.globalResistanceOverflowMultiplier, 0.35D), 0.0D, 1.0D);
    }

    public static double globalResistanceHardCap() {
        return clamp(orDefault(gameplay.globalResistanceHardCap, 0.35D), 0.0D, 0.95D);
    }

    public static double globalResistanceFullEffectDamage() {
        return Math.max(0.1D, orDefault(gameplay.globalResistanceFullEffectDamage, 8.0D));
    }

    public static double globalResistanceMinimumDamageScale() {
        return clamp(orDefault(gameplay.globalResistanceMinimumDamageScale, 0.25D), 0.0D, 1.0D);
    }

    public static double globalResistanceEnvironmentalMultiplier() {
        return clamp(orDefault(gameplay.globalResistanceEnvironmentalMultiplier, 0.25D), 0.0D, 1.0D);
    }

    public static double specificResistanceHardCap() {
        return clamp(orDefault(gameplay.specificResistanceHardCap, 0.85D), 0.0D, 0.95D);
    }

    public static double provocadorBaseAggroRange() {
        return Math.max(1.0D, orDefault(gameplay.provocadorBaseAggroRange, 15.0D));
    }

    public static int titanMaxStacks() {
        return Math.max(1, gameplay.titanMaxStacks == null ? maxPartySize() : gameplay.titanMaxStacks);
    }

    public static double titanGlobalDamagePerStack() {
        return orDefault(gameplay.titanGlobalDamagePerStack, 0.025D);
    }

    public static double titanGlobalCritDamagePerStack() {
        return orDefault(gameplay.titanGlobalCritDamagePerStack, 0.025D);
    }

    public static double titanGlobalAttackSpeedPerStack() {
        return orDefault(gameplay.titanGlobalAttackSpeedPerStack, 0.0125D);
    }

    public static double titanMoveSpeedPerStack() {
        return orDefault(gameplay.titanMoveSpeedPerStack, 0.0125D);
    }

    public static double titanGlobalResistancePerStack() {
        return orDefault(gameplay.titanGlobalResistancePerStack, 0.0125D);
    }

    public static double titanGlobalCritChancePerStack() {
        return orDefault(gameplay.titanGlobalCritChancePerStack, 0.0125D);
    }

    public static double titanHealthPerStack() {
        return orDefault(gameplay.titanHealthPerStack, 1.0D);
    }

    public static String configDirForDisplay() {
        return CONFIG_DIR.toString();
    }

    private static <T> T loadOrWriteDefault(Path path, Class<T> type, T defaultValue) throws IOException {
        if (!Files.exists(path)) {
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(defaultValue, writer);
            }
            return defaultValue;
        }
        try (Reader reader = Files.newBufferedReader(path)) {
            T loaded = GSON.fromJson(reader, type);
            return loaded == null ? defaultValue : loaded;
        }
    }

    private static Map<String, RequirementEntry> loadOrWriteDefaultRequirements() throws IOException {
        if (!Files.exists(REQUIREMENTS_PATH)) {
            Map<String, RequirementEntry> defaults = defaultRequirements();
            try (Writer writer = Files.newBufferedWriter(REQUIREMENTS_PATH)) {
                GSON.toJson(defaults, writer);
            }
            return defaults;
        }

        try (Reader reader = Files.newBufferedReader(REQUIREMENTS_PATH)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (!root.isJsonObject()) {
                return defaultRequirements();
            }

            JsonObject object = root.getAsJsonObject();
            JsonObject requirementObject = object.has("requirements") && object.get("requirements").isJsonObject()
                    ? object.getAsJsonObject("requirements")
                    : object;

            Map<String, RequirementEntry> loaded = new LinkedHashMap<>();
            for (var entry : requirementObject.entrySet()) {
                if (entry.getValue().isJsonObject()) {
                    RequirementEntry requirement = GSON.fromJson(entry.getValue(), RequirementEntry.class);
                    if (requirement != null) {
                        loaded.put(entry.getKey(), requirement);
                    }
                }
            }
            return loaded.isEmpty() ? defaultRequirements() : loaded;
        }
    }

    private static GameplayFile sanitize(GameplayFile loaded, GameplayFile fallback) {
        return loaded == null ? fallback : loaded;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static Map<String, RequirementEntry> defaultRequirements() {
        Map<String, RequirementEntry> file = new LinkedHashMap<>();
        put(file, "inicio", 0);
        put(file, "combate", 3);
        put(file, "cazador", 5);
        put(file, "bastion", 7);

        put(file, "punteria", 10, "mowziesmobs:naga");
        put(file, "ojo_certero", 15);
        put(file, "tiro_rapido", 15);
        put(file, "francotirador", 20, "cataclysm:ignis");
        put(file, "aljaba_ligera", 20, "bosses_of_mass_destruction:void_blossom");
        put(file, "perforador", 25);
        put(file, "escaramuzador", 25);
        put(file, "deadeye", 30, "cataclysm:scylla");
        put(file, "barrage", 30, "cataclysm:the_leviathan");
        put(file, "maestro_tirador", 35, "minecraft:ender_dragon");

        put(file, "luchador", 5);
        put(file, "coraza_de_acero", 10, "mowziesmobs:umvuthi");
        put(file, "danzante_de_acero", 15);
        put(file, "verdugo", 15);
        put(file, "inamovible", 17, "cataclysm:netherite_monstrosity");
        put(file, "berserker", 20);
        put(file, "juggernaut", 20);
        put(file, "veterano_de_guerra", 25);
        put(file, "conquistador", 30, "cataclysm:maledictus");
        put(file, "senor_de_la_guerra", 35, "minecraft:warden");

        put(file, "muralla", 10, "mowziesmobs:ferrous_wroughtnaut");
        put(file, "fortaleza", 15);
        put(file, "guardaespaldas", 15);
        put(file, "coloso", 20, "cataclysm:netherite_monstrosity");
        put(file, "provocador", 20, "bosses_of_mass_destruction:void_blossom");
        put(file, "regenerador", 25);
        put(file, "invencible", 25);
        put(file, "bastion_absoluto", 30, "cataclysm:scylla");
        put(file, "titan", 35, "minecraft:wither");
        return file;
    }

    private static GameplayFile defaultGameplay() {
        GameplayFile file = new GameplayFile();
        file.disableVanillaJumpCriticals = true;
        file.disableVanillaFullyChargedBowCriticals = true;
        file.bossCreditWindowSeconds = 300;
        file.maxSoloBossCreditPlayers = 3;
        file.minPartySize = 2;
        file.maxPartySize = 4;
        file.globalResistanceSoftCap = 0.20D;
        file.globalResistanceOverflowMultiplier = 0.35D;
        file.globalResistanceHardCap = 0.35D;
        file.globalResistanceFullEffectDamage = 8.0D;
        file.globalResistanceMinimumDamageScale = 0.25D;
        file.globalResistanceEnvironmentalMultiplier = 0.25D;
        file.specificResistanceHardCap = 0.85D;
        file.provocadorBaseAggroRange = 15.0D;
        file.titanMaxStacks = 4;
        file.titanGlobalDamagePerStack = 0.025D;
        file.titanGlobalCritDamagePerStack = 0.025D;
        file.titanGlobalAttackSpeedPerStack = 0.0125D;
        file.titanMoveSpeedPerStack = 0.0125D;
        file.titanGlobalResistancePerStack = 0.0125D;
        file.titanGlobalCritChancePerStack = 0.0125D;
        file.titanHealthPerStack = 1.0D;
        return file;
    }

    private static void put(Map<String, RequirementEntry> file, String skillId, int levels) {
        file.put(skillId, new RequirementEntry(levels, null));
    }

    private static void put(Map<String, RequirementEntry> file, String skillId, int levels, String boss) {
        file.put(skillId, new RequirementEntry(levels, boss));
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double orDefault(Double value, double fallback) {
        return value == null ? fallback : value;
    }

    private static final class RequirementEntry {
        private int levels;
        private String boss;

        private RequirementEntry() {
        }

        private RequirementEntry(int levels, String boss) {
            this.levels = levels;
            this.boss = boss;
        }
    }

    private static final class GameplayFile {
        @SerializedName(value = "disable_vanilla_jump_criticals", alternate = "disableVanillaJumpCriticals")
        private boolean disableVanillaJumpCriticals;
        @SerializedName(value = "disable_vanilla_fully_charged_bow_criticals", alternate = "disableVanillaFullyChargedBowCriticals")
        private boolean disableVanillaFullyChargedBowCriticals;
        @SerializedName(value = "boss_credit_window_seconds", alternate = "bossCreditWindowSeconds")
        private int bossCreditWindowSeconds;
        @SerializedName(value = "max_solo_boss_credit_players", alternate = "maxSoloBossCreditPlayers")
        private int maxSoloBossCreditPlayers;
        @SerializedName(value = "min_party_size", alternate = "minPartySize")
        private int minPartySize;
        @SerializedName(value = "max_party_size", alternate = "maxPartySize")
        private int maxPartySize;
        @SerializedName(value = "global_resistance_soft_cap", alternate = "globalResistanceSoftCap")
        private Double globalResistanceSoftCap;
        @SerializedName(value = "global_resistance_overflow_multiplier", alternate = "globalResistanceOverflowMultiplier")
        private Double globalResistanceOverflowMultiplier;
        @SerializedName(value = "global_resistance_hard_cap", alternate = "globalResistanceHardCap")
        private Double globalResistanceHardCap;
        @SerializedName(value = "global_resistance_full_effect_damage", alternate = "globalResistanceFullEffectDamage")
        private Double globalResistanceFullEffectDamage;
        @SerializedName(value = "global_resistance_minimum_damage_scale", alternate = "globalResistanceMinimumDamageScale")
        private Double globalResistanceMinimumDamageScale;
        @SerializedName(value = "global_resistance_environmental_multiplier", alternate = "globalResistanceEnvironmentalMultiplier")
        private Double globalResistanceEnvironmentalMultiplier;
        @SerializedName(value = "specific_resistance_hard_cap", alternate = "specificResistanceHardCap")
        private Double specificResistanceHardCap;
        @SerializedName(value = "provocador_base_aggro_range", alternate = "provocadorBaseAggroRange")
        private Double provocadorBaseAggroRange;
        @SerializedName(value = "titan_max_stacks", alternate = "titanMaxStacks")
        private Integer titanMaxStacks;
        @SerializedName(value = "titan_global_damage_per_stack", alternate = "titanGlobalDamagePerStack")
        private Double titanGlobalDamagePerStack;
        @SerializedName(value = "titan_global_crit_damage_per_stack", alternate = "titanGlobalCritDamagePerStack")
        private Double titanGlobalCritDamagePerStack;
        @SerializedName(value = "titan_global_attack_speed_per_stack", alternate = "titanGlobalAttackSpeedPerStack")
        private Double titanGlobalAttackSpeedPerStack;
        @SerializedName(value = "titan_move_speed_per_stack", alternate = "titanMoveSpeedPerStack")
        private Double titanMoveSpeedPerStack;
        @SerializedName(value = "titan_global_resistance_per_stack", alternate = "titanGlobalResistancePerStack")
        private Double titanGlobalResistancePerStack;
        @SerializedName(value = "titan_global_crit_chance_per_stack", alternate = "titanGlobalCritChancePerStack")
        private Double titanGlobalCritChancePerStack;
        @SerializedName(value = "titan_health_per_stack", alternate = "titanHealthPerStack")
        private Double titanHealthPerStack;
    }
}
