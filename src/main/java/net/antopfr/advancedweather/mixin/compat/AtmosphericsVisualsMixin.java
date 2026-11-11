package net.antopfr.advancedweather.mixin.compat;

import net.antopfr.advancedweather.compat.atmospherics.AtmosphericsCompat;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.beash.atmospherics.util.AtmosphericEnvironmentVisuals", remap = false)
public abstract class AtmosphericsVisualsMixin {

    @Unique
    private static final String PLACE = "(Lnet/minecraft/client/multiplayer/ClientLevel;Lnet/minecraft/world/phys/Vec3;";
    @Unique
    private static final String FOG_COLOR = "getFogColor" + PLACE + "I)I";
    @Unique
    private static final String FOG_START = "getFogStart" + PLACE + "F)F";
    @Unique
    private static final String FOG_END = "getFogEnd" + PLACE + "F)F";
    @Unique
    private static final String SKY_COLOR = "getSkyColor" + PLACE + "I)I";
    @Unique
    private static final String TWILIGHT_COLOR = "getSunriseSunsetColor" + PLACE + "I)I";

    @Inject(method = {FOG_COLOR, FOG_START, FOG_END, SKY_COLOR, TWILIGHT_COLOR}, at = @At("HEAD"), require = 0)
    private static void advancedweather$enter(CallbackInfoReturnable<?> cir) {
        AtmosphericsCompat.enterVisuals();
    }

    @Inject(method = {FOG_COLOR, FOG_START, FOG_END, SKY_COLOR, TWILIGHT_COLOR}, at = @At("RETURN"), require = 0)
    private static void advancedweather$exit(CallbackInfoReturnable<?> cir) {
        AtmosphericsCompat.exitVisuals();
    }

    @Inject(method = FOG_START, at = @At("RETURN"), cancellable = true, require = 0)
    private static void advancedweather$fogStart(ClientLevel level, Vec3 place, float vanilla, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(AtmosphericsCompat.fogStart(cir.getReturnValueF()));
    }

    @Inject(method = FOG_END, at = @At("RETURN"), cancellable = true, require = 0)
    private static void advancedweather$fogEnd(ClientLevel level, Vec3 place, float vanilla, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(AtmosphericsCompat.fogEnd(cir.getReturnValueF()));
    }

    @Inject(method = FOG_COLOR, at = @At("RETURN"), cancellable = true, require = 0)
    private static void advancedweather$fogColor(ClientLevel level, Vec3 place, int vanilla, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(AtmosphericsCompat.fogColor(cir.getReturnValueI(), vanilla));
    }
}
