package net.antopfr.advancedweather.mixin.compat;

import net.antopfr.advancedweather.compat.particlerain.ParticleRainCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "pigcart.particlerain.WindManager", remap = false)
public abstract class ParticleRainWindMixin {

    @Unique
    private static final String WIND_MULTIPLIER = "windMultiplier(Lnet/minecraft/client/multiplayer/ClientLevel;"
            + "Lpigcart/particlerain/config/ParticleData;)F";

    @Inject(method = WIND_MULTIPLIER, at = @At("HEAD"), require = 0)
    private static void advancedweather$enter(CallbackInfoReturnable<Float> cir) {
        ParticleRainCompat.enter();
    }

    @Inject(method = WIND_MULTIPLIER, at = @At("RETURN"), require = 0)
    private static void advancedweather$exit(CallbackInfoReturnable<Float> cir) {
        ParticleRainCompat.exit();
    }
}
