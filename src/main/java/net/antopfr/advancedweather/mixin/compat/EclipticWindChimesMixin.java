package net.antopfr.advancedweather.mixin.compat;

import net.antopfr.advancedweather.compat.eclipticseasons.EclipticSeasonsCompat;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.teamtea.eclipticseasons.common.block.blockentity.WindChimesBlockEntity", remap = false)
public abstract class EclipticWindChimesMixin {

    @Unique
    private static final int REFRESH_TICKS = 10;

    @Unique
    private long advancedweather$windRead = Long.MIN_VALUE;
    @Unique
    private boolean advancedweather$rungByWind;

    @Inject(method = "isShaking()Z", at = @At("RETURN"), cancellable = true, require = 0)
    private void advancedweather$rungByWind(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) return;

        BlockEntity chimes = (BlockEntity) (Object) this;
        Level level = chimes.getLevel();
        if (level == null || !level.isClientSide) return;

        long now = level.getGameTime();
        if (now < advancedweather$windRead || now - advancedweather$windRead >= REFRESH_TICKS) {
            advancedweather$windRead = now;
            advancedweather$rungByWind = EclipticSeasonsCompat.ringsChimes(level, chimes.getBlockPos(), advancedweather$rungByWind);
        }
        if (advancedweather$rungByWind) cir.setReturnValue(true);
    }
}
