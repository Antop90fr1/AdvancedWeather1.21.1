package net.antopfr.advancedweather.mixin.compat;

import net.antopfr.advancedweather.compat.eclipticseasons.EclipticSnowChoice;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.teamtea.eclipticseasons.client.core.ExtraModelManager", remap = false)
public abstract class EclipticSnowChoiceMixin {

    @Inject(method = "maySnowyAt", at = @At("HEAD"), cancellable = true, require = 0)
    private static void advancedweather$maySnowyAt(Level level, @Coerce Object slice, BlockState state, BlockPos pos,
                                                   RandomSource random, long seed, CallbackInfoReturnable<Boolean> cir) {
        if (EclipticSnowChoice.ours(state)) cir.setReturnValue(false);
    }

    @Inject(method = "canSnowy", at = @At("HEAD"), cancellable = true, require = 0)
    private static void advancedweather$canSnowy(BlockAndTintGetter getter, BlockPos pos, BlockState state, long seed,
                                                 BlockPos.MutableBlockPos check, CallbackInfoReturnable<Boolean> cir) {
        if (EclipticSnowChoice.ours(state)) cir.setReturnValue(false);
    }
}
