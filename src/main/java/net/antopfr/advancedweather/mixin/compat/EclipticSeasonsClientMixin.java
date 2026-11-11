package net.antopfr.advancedweather.mixin.compat;

import net.antopfr.advancedweather.client.state.ClientWeatherState;
import net.antopfr.advancedweather.compat.eclipticseasons.EclipticSeasonsCompat;
import net.antopfr.advancedweather.compat.particlerain.ParticleRainCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = {
        "com.teamtea.eclipticseasons.api.util.EclipticUtil",
        "com.teamtea.eclipticseasons.common.core.biome.WeatherManager"
}, remap = false)
public abstract class EclipticSeasonsClientMixin {

    @Unique
    private static final String ASKED = "(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/biome/Biome;"
            + "Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/biome/Biome$Precipitation;";

    @Inject(method = {"getRainOrSnow" + ASKED, "getPrecipitationAt" + ASKED}, at = @At("RETURN"), cancellable = true, require = 0)
    private static void advancedweather$clientPrecipitation(Level level, Biome biome, BlockPos pos,
                                                            CallbackInfoReturnable<Biome.Precipitation> cir) {
        if (level != null ? !level.isClientSide : !Minecraft.getInstance().isSameThread()) return;

        Biome.Precipitation ours = advancedweather$falling();
        if (ours != null) cir.setReturnValue(ours);
    }

    @Unique
    private static Biome.Precipitation advancedweather$falling() {
        if (ParticleRainCompat.asking()) {
            Biome.Precipitation seen = ParticleRainCompat.precipitation();
            if (seen != null) return seen;
        }
        return EclipticSeasonsCompat.precipitation(ClientWeatherState.precipitating());
    }
}
