package net.antopfr.advancedweather.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.antopfr.advancedweather.client.render.SkyVeil;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LevelRenderer.class)
public class StarRenderMixin {

    @ModifyExpressionValue(
            method = "renderSky",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/ClientLevel;getStarBrightness(F)F"
            )
    )
    private float aw_hideStars(float brightness, @Local(argsOnly = true) float partialTick) {
        return brightness * SkyVeil.visibility(partialTick);
    }
}
