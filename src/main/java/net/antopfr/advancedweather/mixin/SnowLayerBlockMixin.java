package net.antopfr.advancedweather.mixin;

import net.antopfr.advancedweather.weather.LocalAtmosphere;
import net.antopfr.advancedweather.weather.SnowMelt;
import net.antopfr.advancedweather.weather.WeatherManager;
import net.antopfr.advancedweather.weather.WeatherTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SnowLayerBlock.class)
public class SnowLayerBlockMixin {

    @Inject(method = "canSurvive", at = @At("HEAD"), cancellable = true)
    private void advancedweather$liesOnIce(BlockState state, LevelReader level, BlockPos pos,
                                          CallbackInfoReturnable<Boolean> cir) {
        BlockPos below = pos.below();
        BlockState ground = level.getBlockState(below);
        if (ground.is(BlockTags.ICE) && Block.isFaceFull(ground.getCollisionShape(level, below), Direction.UP)) {
            cir.setReturnValue(true);
        }
    }

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
            int layers = state.getValue(SnowLayerBlock.LAYERS);
            float crust = WeatherManager.get(level).getSnowPack(level).crust();
            if (random.nextFloat() < SnowMelt.perRandomTick(level, localTemperature, crust) * layers) {
                if (layers > 1) {
                    level.setBlock(pos, state.setValue(SnowLayerBlock.LAYERS, layers - 1), 2);
                } else {
                    level.removeBlock(pos, false);
                }
            }
            ci.cancel();
        } else {
            if (level.getBrightness(LightLayer.BLOCK, pos) > 11) {
                level.removeBlock(pos, false);
            }
            ci.cancel();
        }
    }
}