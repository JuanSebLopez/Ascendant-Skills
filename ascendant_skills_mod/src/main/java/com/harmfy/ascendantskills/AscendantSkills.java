package com.harmfy.ascendantskills;

import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(AscendantSkills.MOD_ID)
public final class AscendantSkills {
    public static final String MOD_ID = "ascendant_skills";
    public static final Logger LOGGER = LogUtils.getLogger();

    public AscendantSkills(IEventBus modEventBus) {
        AscendantConfig.loadOrCreate();
        AscendantAttributes.ATTRIBUTES.register(modEventBus);
        modEventBus.addListener(AscendantAttributes::addPlayerAttributes);
        modEventBus.addListener(AscendantNetworking::register);
        Attributes.KNOCKBACK_RESISTANCE.value().setSyncable(true);

        NeoForge.EVENT_BUS.addListener(AscendantCommands::register);
        NeoForge.EVENT_BUS.addListener(BossTracker::onLivingDamage);
        NeoForge.EVENT_BUS.addListener(BossTracker::onLivingDeath);
        NeoForge.EVENT_BUS.addListener(CombatPerks::onLivingIncomingDamage);
        NeoForge.EVENT_BUS.addListener(CombatPerks::onLivingDamagePost);
        NeoForge.EVENT_BUS.addListener(CombatPerks::onLivingDeath);
        NeoForge.EVENT_BUS.addListener(CombatPerks::onLivingKnockBack);
        NeoForge.EVENT_BUS.addListener(CombatPerks::onLivingUseItemTick);
        NeoForge.EVENT_BUS.addListener(CombatPerks::onArrowLoose);
        NeoForge.EVENT_BUS.addListener(CombatPerks::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(CriticalControl::onCriticalHit);
        NeoForge.EVENT_BUS.addListener(CriticalControl::onEntityJoinLevel);
        NeoForge.EVENT_BUS.addListener(CriticalControl::onProjectileImpact);
        NeoForge.EVENT_BUS.addListener(PuffishBridge::onPlayerLogin);
        NeoForge.EVENT_BUS.addListener(PuffishBridge::onPlayerTick);

        if (ModList.get().isLoaded("puffish_skills")) {
            PuffishBridge.register();
            LOGGER.info("Ascendant Skills loaded with Puffish Skills integration.");
        } else {
            LOGGER.warn("Puffish Skills was not found. Ascendant commands and boss progress will work, but skill purchase integration is disabled.");
        }
    }
}
