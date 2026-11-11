package net.antopfr.advancedweather.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.antopfr.advancedweather.compat.atmospherics.AtmosphericsCompat;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LevelRenderer.class)
public class SkyPassMixin {

    @WrapOperation(
            method = "renderLevel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/LevelRenderer;renderSky(Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;FLnet/minecraft/client/Camera;ZLjava/lang/Runnable;)V"
            ),
            require = 0
    )
    private void advancedweather$skyPass(LevelRenderer renderer, Matrix4f frustumMatrix, Matrix4f projectionMatrix,
                                         float partialTick, Camera camera, boolean foggy, Runnable skyFogSetup,
                                         Operation<Void> original) {
        AtmosphericsCompat.enterSkyPass();
        try {
            original.call(renderer, frustumMatrix, projectionMatrix, partialTick, camera, foggy, skyFogSetup);
        } finally {
            AtmosphericsCompat.exitSkyPass();
        }
    }
}
