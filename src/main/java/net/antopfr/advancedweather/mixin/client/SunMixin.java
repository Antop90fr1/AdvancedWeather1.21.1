package net.antopfr.advancedweather.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.systems.RenderSystem;
import net.antopfr.advancedweather.client.render.SkyVeil;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class SunMixin {

    @Unique
    private static final ResourceLocation AW_SUN = ResourceLocation.withDefaultNamespace("textures/environment/sun.png");
    @Unique
    private static final ResourceLocation AW_MOON = ResourceLocation.withDefaultNamespace("textures/environment/moon_phases.png");

    @Unique
    private float advancedWeather1_21_1$partialTick = 1.0f;

    @Inject(method = "renderSky", at = @At("HEAD"))
    private void aw_capturePartialTick(Matrix4f frustumMatrix, Matrix4f projectionMatrix, float partialTick, Camera camera, boolean isFoggy, Runnable skyFogSetup, CallbackInfo ci) {
        this.advancedWeather1_21_1$partialTick = partialTick;
    }

    @WrapOperation(
            method = "renderSky",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderTexture(ILnet/minecraft/resources/ResourceLocation;)V"
            )
    )
    private void aw_hideSunMoon(int unit, ResourceLocation texture, Operation<Void> original) {
        original.call(unit, texture);
        if (texture.equals(AW_SUN) || texture.equals(AW_MOON)) {
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, SkyVeil.visibility(advancedWeather1_21_1$partialTick));
        }
    }
}
