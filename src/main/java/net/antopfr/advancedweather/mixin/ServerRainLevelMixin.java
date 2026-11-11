package net.antopfr.advancedweather.mixin;

import net.antopfr.advancedweather.weather.ServerRain;
import net.antopfr.advancedweather.weather.WeatherTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public class ServerRainLevelMixin {

    @Inject(method = "getRainLevel", at = @At("HEAD"), cancellable = true)
    private void advancedweather$serverRain(float delta, CallbackInfoReturnable<Float> cir) {
        Level level = (Level) (Object) this;
        if (level.isClientSide) return;

        float rain = ServerRain.of(level);
        if (rain >= 0f) cir.setReturnValue(rain);
    }

    @Inject(method = "isRainingAt", at = @At("HEAD"), cancellable = true)
    private void advancedweather$rainAnywhere(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        Level level = (Level) (Object) this;
        if (level.isClientSide) return;

        WeatherTypes weather = ServerRain.weatherOf(level);
        if (weather == null || weather.precipitation() == Biome.Precipitation.NONE) return;

        cir.setReturnValue(weather.precipitation() == Biome.Precipitation.RAIN
                && level.canSeeSky(pos)
                && level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, pos).getY() <= pos.getY());
    }
}
