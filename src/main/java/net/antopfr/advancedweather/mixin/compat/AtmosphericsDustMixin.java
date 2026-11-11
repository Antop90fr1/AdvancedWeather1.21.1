package net.antopfr.advancedweather.mixin.compat;

import net.antopfr.advancedweather.compat.atmospherics.AtmosphericsCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.beash.atmospherics.particle.DryWeatherParticleSpawner", remap = false)
public abstract class AtmosphericsDustMixin {

    @Unique
    private static final String TICK = "tick(Lcom/beash/atmospherics/particle/EmitterContext;)V";

    @Inject(method = TICK, at = @At("HEAD"), cancellable = true, require = 0)
    private void advancedweather$enter(CallbackInfo ci) {
        if (AtmosphericsCompat.dustWashedOut()) {
            ci.cancel();
            return;
        }
        AtmosphericsCompat.enterDry();
    }

    @Inject(method = TICK, at = @At("RETURN"), require = 0)
    private void advancedweather$exit(CallbackInfo ci) {
        AtmosphericsCompat.exitDry();
    }
}
