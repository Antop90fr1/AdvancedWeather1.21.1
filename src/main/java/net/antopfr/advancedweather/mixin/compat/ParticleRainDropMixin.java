package net.antopfr.advancedweather.mixin.compat;

import net.antopfr.advancedweather.compat.particlerain.ParticleRainDrop;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "pigcart.particlerain.particle.CustomParticle", remap = false)
public abstract class ParticleRainDropMixin extends TextureSheetParticle implements ParticleRainDrop {

    @Unique
    private ParticleRainDrop.Landing advancedweather$landing;

    protected ParticleRainDropMixin(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z);
    }

    @Override
    public void advancedweather$onLanding(ParticleRainDrop.Landing landing) {
        advancedweather$landing = landing;
    }

    @Override
    protected int getLightColor(float partialTick) {
        return advancedweather$landing != null ? LightTexture.FULL_BRIGHT : super.getLightColor(partialTick);
    }

    @Inject(method = "onCollision(Lnet/minecraft/world/phys/BlockHitResult;)V", at = @At("TAIL"), require = 0)
    private void advancedweather$land(BlockHitResult hit, CallbackInfo ci) {
        if (advancedweather$landing == null) return;

        Vec3 where = hit.getLocation();
        advancedweather$landing.land(this.level, where.x, where.y, where.z);
    }
}
