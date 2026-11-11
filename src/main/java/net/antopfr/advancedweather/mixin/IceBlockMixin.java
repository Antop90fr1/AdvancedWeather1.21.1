package net.antopfr.advancedweather.mixin;

import net.antopfr.advancedweather.weather.LocalAtmosphere;
import net.antopfr.advancedweather.weather.SnowMelt;
import net.antopfr.advancedweather.weather.WeatherTypes;
import net.antopfr.advancedweather.weather.WeatherManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.IceBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(IceBlock.class)
public class IceBlockMixin {

    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    private void onRandomTick(BlockState state, ServerLevel level, BlockPos pos,
                              RandomSource random, CallbackInfo ci) {

        WeatherTypes type = WeatherManager.get(level).getCurrentWeather(level);

        if (type == WeatherTypes.SNOW || type == WeatherTypes.BLIZZARD || type == WeatherTypes.FREEZING_RAIN) {
            ci.cancel();
            return;
        }

        if (level.getBiome(pos).value().coldEnoughToSnow(pos)) {
            ci.cancel();
            return;
        }

        float localTemperature = LocalAtmosphere.getLocalTemperature(level, pos);

        if (localTemperature > 0f) {
            if (random.nextFloat() < SnowMelt.perRandomTick(level, localTemperature, 0f)) {
                ((IceBlockAccessor) this).invokeMelt(state, level, pos);
            }
            ci.cancel();
        } else {
            int blockLight = level.getBrightness(LightLayer.BLOCK, pos);
            int threshold = 11 - state.getLightBlock(level, pos);

            if (blockLight > threshold) {
                ((IceBlockAccessor) this).invokeMelt(state, level, pos);
            }

            ci.cancel();
        }
    }
}