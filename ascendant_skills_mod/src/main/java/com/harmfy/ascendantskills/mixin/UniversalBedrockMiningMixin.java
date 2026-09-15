package com.harmfy.ascendantskills.mixin;

import com.harmfy.ascendantskills.MiningPerks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.class)
public abstract class UniversalBedrockMiningMixin {
    @Inject(method = "getDestroyProgress", at = @At("HEAD"), cancellable = true)
    private void ascendantSkills$bedrockDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos,
                                                       CallbackInfoReturnable<Float> cir) {
        float progress = MiningPerks.universalDestroyProgress(state, player, level, pos);
        if (progress >= 0.0F) {
            cir.setReturnValue(progress);
        }
    }
}
