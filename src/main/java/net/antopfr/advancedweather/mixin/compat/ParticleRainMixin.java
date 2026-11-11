package net.antopfr.advancedweather.mixin.compat;

import net.antopfr.advancedweather.compat.particlerain.ParticleRainCompat;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "pigcart.particlerain.ParticleRain", remap = false)
public abstract class ParticleRainMixin {

    @Inject(method = "onTick(Lnet/minecraft/client/Minecraft;)V", at = @At("HEAD"), require = 0)
    private static void advancedweather$enter(CallbackInfo ci) {
        ParticleRainCompat.enter();
    }

    @Inject(method = "onTick(Lnet/minecraft/client/Minecraft;)V", at = @At("RETURN"), require = 0)
    private static void advancedweather$exit(CallbackInfo ci) {
        ParticleRainCompat.exit();
    }

    @Inject(method = "getWind(DDD)Lorg/joml/Vector3f;", at = @At("HEAD"), cancellable = true, require = 0)
    private static void advancedweather$wind(double x, double y, double z, CallbackInfoReturnable<Vector3f> cir) {
        if (ParticleRainCompat.enabled()) cir.setReturnValue(ParticleRainCompat.wind(x, y, z));
    }
}
