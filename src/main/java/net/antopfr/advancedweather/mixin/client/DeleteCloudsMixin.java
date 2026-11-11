package net.antopfr.advancedweather.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.antopfr.advancedweather.compat.iris.IrisCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class DeleteCloudsMixin {
    @Inject(
            method = "renderClouds",
            at = @At("HEAD"),
            cancellable = true
    )
    private void cancelVanillaClouds(PoseStack poseStack, Matrix4f modelMatrix, Matrix4f projectionMatrix, float partialTick, double camX, double camY, double camZ, CallbackInfo ci) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || IrisCompat.shaderPackInUse()) return;

        ci.cancel();
    }
}
