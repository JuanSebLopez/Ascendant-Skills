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
import java.util.List;
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
    private static final Path NATURE_PATH = CONFIG_DIR.resolve("nature.json");
    private static Map<String, RequirementEntry> requirements = defaultRequirements();
    private static GameplayFile gameplay = defaultGameplay();
    private static NatureFile nature = defaultNature();

    private AscendantConfig() {
    }

    public static void loadOrCreate() {
        try {
            Files.createDirectories(CONFIG_DIR);
            requirements = loadOrWriteDefaultRequirements();
            gameplay = sanitize(loadOrWriteDefault(GAMEPLAY_PATH, GameplayFile.class, defaultGameplay()), defaultGameplay());
            nature = sanitizeNature(loadOrWriteDefault(NATURE_PATH, NatureFile.class, defaultNature()), defaultNature());
            writeConfig(NATURE_PATH, nature);
            AscendantSkills.LOGGER.info("Loaded Ascendant Skills config from {}", CONFIG_DIR);
        } catch (IOException | RuntimeException ex) {
            AscendantSkills.LOGGER.error("Failed to load Ascendant Skills config. Using in-memory defaults.", ex);
            requirements = defaultRequirements();
            gameplay = defaultGameplay();
            nature = defaultNature();
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

    public static Set<String> natureRawFoodItems() {
        return Set.copyOf(nature.rawFoodItems);
    }

    public static Set<String> natureNeutralMobs() {
        return Set.copyOf(nature.neutralMobs);
    }

    public static Set<String> natureCreeperAvoidMobs() {
        return Set.copyOf(nature.apexPredatorAvoidMobs);
    }

    public static Set<String> natureCropBonusDrops() {
        return Set.copyOf(nature.cropBonusDrops);
    }

    public static Set<String> natureGrowableCropBlocks() {
        return Set.copyOf(nature.growableCropBlocks);
    }

    public static Set<String> natureNaturalGroundBlocks() {
        return Set.copyOf(nature.naturalGroundBlocks);
    }

    public static Set<String> natureMilkableMobs() {
        return Set.copyOf(nature.milkableMobs);
    }

    public static int natureCropScanIntervalTicks() {
        return Math.max(1, nature.cropScanIntervalTicks);
    }

    public static int natureMaxCropsAdvancedPerScan() {
        return Math.max(1, nature.maxCropsAdvancedPerScan);
    }

    public static double natureCropSecondsPerStageAtSpeedOne() {
        return Math.max(1.0D, nature.cropSecondsPerStageAtSpeedOne);
    }

    public static int natureAnimalScanIntervalTicks() {
        return Math.max(1, nature.animalScanIntervalTicks);
    }

    public static int natureMaxAnimalAccelerationsPerScan() {
        return Math.max(1, nature.maxAnimalAccelerationsPerScan);
    }

    public static double natureBreedingSpeedScale() {
        return Math.max(0.0D, nature.breedingSpeedScale);
    }

    public static double natureForestSoulSpeed() {
        return nature.forestSoulSpeed;
    }

    public static int natureLongEffectRefreshTicks() {
        return Math.max(20, nature.longEffectRefreshTicks);
    }

    public static double natureDruidLeafSpeed() {
        return nature.druidLeafSpeed;
    }

    public static double natureDruidLeafJumpStrength() {
        return nature.druidLeafJumpStrength;
    }

    public static double natureAvatarCrouchSpeed() {
        return nature.avatarCrouchSpeed;
    }

    public static int natureRatelRetaliationTicks() {
        return Math.max(20, nature.retaliationTicks);
    }

    public static double natureGreenHeartClearMoveSpeed() {
        return nature.greenHeartClearMoveSpeed;
    }

    public static double natureGreenHeartClearStepHeight() {
        return nature.greenHeartClearStepHeight;
    }

    public static double natureGreenHeartRainRegen() {
        return nature.greenHeartRainRegen;
    }

    public static double natureGreenHeartRainRegenIntervalReduction() {
        return Math.max(0.0D, nature.greenHeartRainRegenIntervalReduction);
    }

    public static double natureGreenHeartSnowToughness() {
        return nature.greenHeartSnowToughness;
    }

    public static double natureGreenHeartSnowArmor() {
        return nature.greenHeartSnowArmor;
    }

    public static double natureGreenHeartStormAttackSpeed() {
        return nature.greenHeartStormAttackSpeed;
    }

    public static double natureGreenHeartStormDamage() {
        return nature.greenHeartStormDamage;
    }

    public static double natureBeastMasterPackRadius() {
        return Math.max(1.0D, nature.beastMasterPackRadius);
    }

    public static int natureBeastMasterPackMaxStacks() {
        return Math.max(1, nature.beastMasterPackMaxStacks);
    }

    public static double natureBeastMasterGlobalDamagePerStack() {
        return nature.beastMasterGlobalDamagePerStack;
    }

    public static double natureBeastMasterArmorPerStack() {
        return nature.beastMasterArmorPerStack;
    }

    public static double natureBeastMasterMoveSpeedPerStack() {
        return nature.beastMasterMoveSpeedPerStack;
    }

    public static double natureBeastMasterAttackSpeedPerStack() {
        return nature.beastMasterAttackSpeedPerStack;
    }

    public static double natureBeastMasterPetHealth() {
        return Math.max(0.0D, nature.beastMasterPetHealth);
    }

    public static double natureBeastMasterPetDamage() {
        return nature.beastMasterPetDamage;
    }

    public static double natureBeastMasterPetArmor() {
        return nature.beastMasterPetArmor;
    }

    public static double natureBeastMasterPetToughness() {
        return nature.beastMasterPetToughness;
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

    private static void writeConfig(Path path, Object value) throws IOException {
        try (Writer writer = Files.newBufferedWriter(path)) {
            GSON.toJson(value, writer);
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

    private static NatureFile sanitizeNature(NatureFile loaded, NatureFile fallback) {
        if (loaded == null) {
            return fallback;
        }
        if (loaded.rawFoodItems == null || loaded.rawFoodItems.isEmpty()) loaded.rawFoodItems = fallback.rawFoodItems;
        if (loaded.neutralMobs == null || loaded.neutralMobs.isEmpty()) loaded.neutralMobs = fallback.neutralMobs;
        if (loaded.apexPredatorAvoidMobs == null || loaded.apexPredatorAvoidMobs.isEmpty()) loaded.apexPredatorAvoidMobs = fallback.apexPredatorAvoidMobs;
        if (loaded.cropBonusDrops == null || loaded.cropBonusDrops.isEmpty()) loaded.cropBonusDrops = fallback.cropBonusDrops;
        if (loaded.growableCropBlocks == null || loaded.growableCropBlocks.isEmpty()) loaded.growableCropBlocks = fallback.growableCropBlocks;
        if (loaded.naturalGroundBlocks == null || loaded.naturalGroundBlocks.isEmpty()) loaded.naturalGroundBlocks = fallback.naturalGroundBlocks;
        if (loaded.milkableMobs == null || loaded.milkableMobs.isEmpty()) loaded.milkableMobs = fallback.milkableMobs;
        if (loaded.cropScanIntervalTicks <= 0) loaded.cropScanIntervalTicks = fallback.cropScanIntervalTicks;
        if (loaded.maxCropsAdvancedPerScan <= 0) loaded.maxCropsAdvancedPerScan = fallback.maxCropsAdvancedPerScan;
        if (loaded.cropSecondsPerStageAtSpeedOne <= 0.0D) loaded.cropSecondsPerStageAtSpeedOne = fallback.cropSecondsPerStageAtSpeedOne;
        if (loaded.animalScanIntervalTicks <= 0) loaded.animalScanIntervalTicks = fallback.animalScanIntervalTicks;
        if (loaded.maxAnimalAccelerationsPerScan <= 0) loaded.maxAnimalAccelerationsPerScan = fallback.maxAnimalAccelerationsPerScan;
        if (loaded.breedingSpeedScale < 0.0D) loaded.breedingSpeedScale = fallback.breedingSpeedScale;
        if (loaded.longEffectRefreshTicks <= 0) loaded.longEffectRefreshTicks = fallback.longEffectRefreshTicks;
        if (loaded.retaliationTicks <= 0) loaded.retaliationTicks = fallback.retaliationTicks;
        if (loaded.greenHeartClearMoveSpeed == 0.0D) loaded.greenHeartClearMoveSpeed = fallback.greenHeartClearMoveSpeed;
        if (loaded.greenHeartClearStepHeight == 0.0D) loaded.greenHeartClearStepHeight = fallback.greenHeartClearStepHeight;
        if (loaded.greenHeartRainRegen == 0.0D) loaded.greenHeartRainRegen = fallback.greenHeartRainRegen;
        if (loaded.greenHeartRainRegenIntervalReduction == 0.0D) loaded.greenHeartRainRegenIntervalReduction = fallback.greenHeartRainRegenIntervalReduction;
        if (loaded.greenHeartSnowToughness == 0.0D) loaded.greenHeartSnowToughness = fallback.greenHeartSnowToughness;
        if (loaded.greenHeartSnowArmor == 0.0D) loaded.greenHeartSnowArmor = fallback.greenHeartSnowArmor;
        if (loaded.greenHeartStormAttackSpeed == 0.0D) loaded.greenHeartStormAttackSpeed = fallback.greenHeartStormAttackSpeed;
        if (loaded.greenHeartStormDamage == 0.0D) loaded.greenHeartStormDamage = fallback.greenHeartStormDamage;
        if (loaded.beastMasterPackRadius <= 0.0D) loaded.beastMasterPackRadius = fallback.beastMasterPackRadius;
        if (loaded.beastMasterPackMaxStacks <= 0) loaded.beastMasterPackMaxStacks = fallback.beastMasterPackMaxStacks;
        if (loaded.beastMasterGlobalDamagePerStack == 0.0D) loaded.beastMasterGlobalDamagePerStack = fallback.beastMasterGlobalDamagePerStack;
        if (loaded.beastMasterArmorPerStack == 0.0D) loaded.beastMasterArmorPerStack = fallback.beastMasterArmorPerStack;
        if (loaded.beastMasterMoveSpeedPerStack == 0.0D) loaded.beastMasterMoveSpeedPerStack = fallback.beastMasterMoveSpeedPerStack;
        if (loaded.beastMasterAttackSpeedPerStack == 0.0D) loaded.beastMasterAttackSpeedPerStack = fallback.beastMasterAttackSpeedPerStack;
        if (loaded.beastMasterPetHealth == 0.0D) loaded.beastMasterPetHealth = fallback.beastMasterPetHealth;
        if (loaded.beastMasterPetDamage == 0.0D) loaded.beastMasterPetDamage = fallback.beastMasterPetDamage;
        if (loaded.beastMasterPetArmor == 0.0D) loaded.beastMasterPetArmor = fallback.beastMasterPetArmor;
        if (loaded.beastMasterPetToughness == 0.0D) loaded.beastMasterPetToughness = fallback.beastMasterPetToughness;
        return loaded;
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

        put(file, "naturaleza", 3);
        put(file, "pacto_natural", 7);
        put(file, "ganadero", 10);
        put(file, "herbolario", 10);
        put(file, "el_buen_vaquero", 15);
        put(file, "el_don_del_ratel", 15, "cataclysm:netherite_monstrosity");
        put(file, "apex_predator", 20);
        put(file, "druida", 20);
        put(file, "guardian_natural", 25);
        put(file, "avatar_de_la_naturaleza", 30, "cataclysm:maledictus");
        put(file, "senor_de_las_bestias", 35, "minecraft:ender_dragon");
        put(file, "espiritu_del_bosque", 35);
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

    private static NatureFile defaultNature() {
        NatureFile file = new NatureFile();
        file.rawFoodItems = List.of(
                "minecraft:apple",
                "minecraft:carrot",
                "minecraft:potato",
                "minecraft:beetroot"
        );
        file.neutralMobs = List.of(
                "minecraft:bee",
                "minecraft:cave_spider",
                "minecraft:dolphin",
                "minecraft:enderman",
                "minecraft:fox",
                "minecraft:goat",
                "minecraft:iron_golem",
                "minecraft:llama",
                "minecraft:panda",
                "minecraft:polar_bear",
                "minecraft:spider",
                "minecraft:wolf",
                "minecraft:zombified_piglin"
        );
        file.apexPredatorAvoidMobs = List.of("minecraft:creeper");
        file.cropBonusDrops = List.of(
                "minecraft:wheat",
                "minecraft:carrot",
                "minecraft:potato",
                "minecraft:beetroot",
                "minecraft:nether_wart",
                "minecraft:cocoa_beans",
                "minecraft:sweet_berries",
                "minecraft:glow_berries"
        );
        file.growableCropBlocks = List.of(
                "minecraft:wheat",
                "minecraft:carrots",
                "minecraft:potatoes",
                "minecraft:beetroots",
                "minecraft:nether_wart",
                "minecraft:cocoa",
                "minecraft:sweet_berry_bush",
                "minecraft:cave_vines",
                "minecraft:cave_vines_plant"
        );
        file.naturalGroundBlocks = List.of(
                "minecraft:grass_block",
                "minecraft:podzol",
                "minecraft:mycelium",
                "minecraft:dirt",
                "minecraft:coarse_dirt",
                "minecraft:rooted_dirt",
                "minecraft:farmland",
                "minecraft:moss_block"
        );
        file.milkableMobs = List.of(
                "minecraft:cow",
                "minecraft:goat",
                "minecraft:mooshroom"
        );
        file.cropScanIntervalTicks = 20;
        file.maxCropsAdvancedPerScan = 64;
        file.cropSecondsPerStageAtSpeedOne = 60.0D;
        file.animalScanIntervalTicks = 20;
        file.maxAnimalAccelerationsPerScan = 32;
        file.breedingSpeedScale = 1.0D;
        file.forestSoulSpeed = 0.05D;
        file.druidLeafSpeed = 0.10D;
        file.druidLeafJumpStrength = 0.12D;
        file.avatarCrouchSpeed = 0.25D;
        file.longEffectRefreshTicks = 200;
        file.retaliationTicks = 600;
        file.greenHeartClearMoveSpeed = 0.05D;
        file.greenHeartClearStepHeight = 1.0D;
        file.greenHeartRainRegen = 0.5D;
        file.greenHeartRainRegenIntervalReduction = 2.0D;
        file.greenHeartSnowToughness = 1.0D;
        file.greenHeartSnowArmor = 2.0D;
        file.greenHeartStormAttackSpeed = 0.05D;
        file.greenHeartStormDamage = 0.05D;
        file.beastMasterPackRadius = 15.0D;
        file.beastMasterPackMaxStacks = 10;
        file.beastMasterGlobalDamagePerStack = 0.01D;
        file.beastMasterArmorPerStack = 0.25D;
        file.beastMasterMoveSpeedPerStack = 0.005D;
        file.beastMasterAttackSpeedPerStack = 0.01D;
        file.beastMasterPetHealth = 12.0D;
        file.beastMasterPetDamage = 0.30D;
        file.beastMasterPetArmor = 6.0D;
        file.beastMasterPetToughness = 2.0D;
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

    private static final class NatureFile {
        @SerializedName(value = "raw_food_items", alternate = "rawFoodItems")
        private List<String> rawFoodItems;
        @SerializedName(value = "neutral_mobs", alternate = "neutralMobs")
        private List<String> neutralMobs;
        @SerializedName(value = "apex_predator_avoid_mobs", alternate = "apexPredatorAvoidMobs")
        private List<String> apexPredatorAvoidMobs;
        @SerializedName(value = "crop_bonus_drops", alternate = "cropBonusDrops")
        private List<String> cropBonusDrops;
        @SerializedName(value = "growable_crop_blocks", alternate = "growableCropBlocks")
        private List<String> growableCropBlocks;
        @SerializedName(value = "natural_ground_blocks", alternate = "naturalGroundBlocks")
        private List<String> naturalGroundBlocks;
        @SerializedName(value = "milkable_mobs", alternate = "milkableMobs")
        private List<String> milkableMobs;
        @SerializedName(value = "crop_scan_interval_ticks", alternate = "cropScanIntervalTicks")
        private int cropScanIntervalTicks;
        @SerializedName(value = "max_crops_advanced_per_scan", alternate = "maxCropsAdvancedPerScan")
        private int maxCropsAdvancedPerScan;
        @SerializedName(value = "crop_seconds_per_stage_at_speed_one", alternate = "cropSecondsPerStageAtSpeedOne")
        private double cropSecondsPerStageAtSpeedOne;
        @SerializedName(value = "animal_scan_interval_ticks", alternate = "animalScanIntervalTicks")
        private int animalScanIntervalTicks;
        @SerializedName(value = "max_animal_accelerations_per_scan", alternate = "maxAnimalAccelerationsPerScan")
        private int maxAnimalAccelerationsPerScan;
        @SerializedName(value = "breeding_speed_scale", alternate = "breedingSpeedScale")
        private double breedingSpeedScale;
        @SerializedName(value = "forest_soul_speed", alternate = "forestSoulSpeed")
        private double forestSoulSpeed;
        @SerializedName(value = "druid_leaf_speed", alternate = "druidLeafSpeed")
        private double druidLeafSpeed;
        @SerializedName(value = "druid_leaf_jump_strength", alternate = "druidLeafJumpStrength")
        private double druidLeafJumpStrength;
        @SerializedName(value = "avatar_crouch_speed", alternate = "avatarCrouchSpeed")
        private double avatarCrouchSpeed;
        @SerializedName(value = "long_effect_refresh_ticks", alternate = "longEffectRefreshTicks")
        private int longEffectRefreshTicks;
        @SerializedName(value = "retaliation_ticks", alternate = "retaliationTicks")
        private int retaliationTicks;
        @SerializedName(value = "green_heart_clear_move_speed", alternate = "greenHeartClearMoveSpeed")
        private double greenHeartClearMoveSpeed;
        @SerializedName(value = "green_heart_clear_step_height", alternate = "greenHeartClearStepHeight")
        private double greenHeartClearStepHeight;
        @SerializedName(value = "green_heart_rain_regen", alternate = "greenHeartRainRegen")
        private double greenHeartRainRegen;
        @SerializedName(value = "green_heart_rain_regen_interval_reduction", alternate = "greenHeartRainRegenIntervalReduction")
        private double greenHeartRainRegenIntervalReduction;
        @SerializedName(value = "green_heart_snow_toughness", alternate = "greenHeartSnowToughness")
        private double greenHeartSnowToughness;
        @SerializedName(value = "green_heart_snow_armor", alternate = "greenHeartSnowArmor")
        private double greenHeartSnowArmor;
        @SerializedName(value = "green_heart_storm_attack_speed", alternate = "greenHeartStormAttackSpeed")
        private double greenHeartStormAttackSpeed;
        @SerializedName(value = "green_heart_storm_damage", alternate = "greenHeartStormDamage")
        private double greenHeartStormDamage;
        @SerializedName(value = "beast_master_pack_radius", alternate = "beastMasterPackRadius")
        private double beastMasterPackRadius;
        @SerializedName(value = "beast_master_pack_max_stacks", alternate = "beastMasterPackMaxStacks")
        private int beastMasterPackMaxStacks;
        @SerializedName(value = "beast_master_global_damage_per_stack", alternate = "beastMasterGlobalDamagePerStack")
        private double beastMasterGlobalDamagePerStack;
        @SerializedName(value = "beast_master_armor_per_stack", alternate = "beastMasterArmorPerStack")
        private double beastMasterArmorPerStack;
        @SerializedName(value = "beast_master_move_speed_per_stack", alternate = "beastMasterMoveSpeedPerStack")
        private double beastMasterMoveSpeedPerStack;
        @SerializedName(value = "beast_master_attack_speed_per_stack", alternate = "beastMasterAttackSpeedPerStack")
        private double beastMasterAttackSpeedPerStack;
        @SerializedName(value = "beast_master_pet_health", alternate = "beastMasterPetHealth")
        private double beastMasterPetHealth;
        @SerializedName(value = "beast_master_pet_damage", alternate = "beastMasterPetDamage")
        private double beastMasterPetDamage;
        @SerializedName(value = "beast_master_pet_armor", alternate = "beastMasterPetArmor")
        private double beastMasterPetArmor;
        @SerializedName(value = "beast_master_pet_toughness", alternate = "beastMasterPetToughness")
        private double beastMasterPetToughness;
    }
}
