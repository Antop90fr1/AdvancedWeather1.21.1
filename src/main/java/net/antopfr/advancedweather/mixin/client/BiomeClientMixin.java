package net.antopfr.advancedweather.mixin.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.antopfr.advancedweather.client.state.ClientWeatherState;
import net.antopfr.advancedweather.compat.particlerain.ParticleRainCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Biome.class, priority = 1100)
public class BiomeClientMixin {

    @Inject(method = "getPrecipitationAt", at = @At("HEAD"), cancellable = true)
    private void onGetPrecipitationAt(BlockPos pos, CallbackInfoReturnable<Biome.Precipitation> cir) {
        if (cir.isCancelled() || !RenderSystem.isOnRenderThread()) return;

        if (ParticleRainCompat.asking()) {
            Biome.Precipitation seen = ParticleRainCompat.precipitation();
            if (seen != null) {
                cir.setReturnValue(seen);
                return;
            }
        }

        Biome.Precipitation falling = ClientWeatherState.precipitating().precipitation();
        if (falling != Biome.Precipitation.NONE) cir.setReturnValue(falling);
    }

    @Inject(method = "hasPrecipitation", at = @At("HEAD"), cancellable = true)
    private void advancedweather$anyBiome(CallbackInfoReturnable<Boolean> cir) {
        if (RenderSystem.isOnRenderThread()
                && ClientWeatherState.precipitating().precipitation() != Biome.Precipitation.NONE) {
            cir.setReturnValue(true);
        }
    }
}