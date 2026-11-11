package net.antopfr.advancedweather.mixin.compat;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.antopfr.advancedweather.compat.atmospherics.AtmosphericsCompat;
import net.minecraft.client.renderer.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = FogRenderer.class, priority = 1100)
public abstract class AtmosphericsTwilightMixin {

    @ModifyExpressionValue(method = "applyTwilightDirectionalFog",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/DimensionSpecialEffects;getSunriseColor(FF)[F"),
            require = 0)
    private static float[] advancedweather$veilTwilight(float[] colors) {
        return AtmosphericsCompat.veiledTwilight(colors);
    }
}
