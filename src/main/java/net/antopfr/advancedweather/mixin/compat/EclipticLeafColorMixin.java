package net.antopfr.advancedweather.mixin.compat;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.teamtea.eclipticseasons.client.color.season.BiomeColorsHandler", remap = false)
public abstract class EclipticLeafColorMixin {

    @Inject(method = "getLeavesColor", at = @At("RETURN"), cancellable = true, require = 0)
    private static void advancedweather$opaque(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(cir.getReturnValue() | 0xFF000000);
    }
}
