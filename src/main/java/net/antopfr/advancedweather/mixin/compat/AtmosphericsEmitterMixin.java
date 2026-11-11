package net.antopfr.advancedweather.mixin.compat;

import net.antopfr.advancedweather.compat.atmospherics.AtmosphericsCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.beash.atmospherics.particle.EmitterContext", remap = false)
public abstract class AtmosphericsEmitterMixin {

    @Inject(method = "rain()F", at = @At("RETURN"), cancellable = true, require = 0)
    private void advancedweather$dryRain(CallbackInfoReturnable<Float> cir) {
        if (AtmosphericsCompat.inDry()) cir.setReturnValue(AtmosphericsCompat.dryRain());
    }
}
