package net.antopfr.advancedweather.mixin.compat;

import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import net.antopfr.advancedweather.compat.sable.ShipCover;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "dev.ryanhcode.sable.sublevel.render.vanilla.VanillaChunkedSubLevelRenderData", remap = false)
public abstract class SableShipCoverMixin {

    @Shadow @Final private Vector3d origin;

    @Shadow public abstract ClientSubLevel getSubLevel();

    @Inject(method = "renderChunkedSubLevel", at = @At("HEAD"), require = 0)
    private void advancedweather$shipGrid(RenderType layer, ShaderInstance shader, Matrix4f modelView,
                                          double camX, double camY, double camZ, CallbackInfo ci) {
        ShipCover.drawingShip(getSubLevel(), origin, shader, camX, camY, camZ);
    }

    @Inject(method = "renderChunkedSubLevel", at = @At("RETURN"), require = 0)
    private void advancedweather$worldGrid(RenderType layer, ShaderInstance shader, Matrix4f modelView,
                                           double camX, double camY, double camZ, CallbackInfo ci) {
        ShipCover.backToWorld(shader);
    }
}
