package net.antopfr.advancedweather.mixin.compat;

import net.antopfr.advancedweather.compat.atmospherics.AtmosphericsCompat;
import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.beash.atmospherics.particle.WindStreakParticle$Factory", remap = false)
public abstract class AtmosphericsWindStreakMixin {

    @Inject(
            method = "createParticle(Lnet/minecraft/core/particles/SimpleParticleType;"
                    + "Lnet/minecraft/client/multiplayer/ClientLevel;DDDDDD)Lnet/minecraft/client/particle/Particle;",
            at = @At("HEAD"), cancellable = true, require = 0
    )
    private void advancedweather$noStreak(CallbackInfoReturnable<Particle> cir) {
        if (AtmosphericsCompat.replacesWindStreaks()) cir.setReturnValue(null);
    }
}
