package com.harmfy.ascendantskills.mixin;

import com.harmfy.ascendantskills.AscendantConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Player.class)
public abstract class PlayerMixin {
    @Redirect(
            method = "attack",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;crit(Lnet/minecraft/world/entity/Entity;)V")
    )
    private void ascendantSkills$hideVanillaJumpCriticalParticles(Player player, Entity target) {
        if (player instanceof ServerPlayer && AscendantConfig.disableVanillaJumpCriticals()) {
            return;
        }
        player.crit(target);
    }
}
