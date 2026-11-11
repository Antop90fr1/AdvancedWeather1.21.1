package net.antopfr.advancedweather.mixin;

import net.antopfr.advancedweather.weather.LocalAtmosphere;
import net.antopfr.advancedweather.weather.SnowMelt;
import net.antopfr.advancedweather.weather.WeatherTypes;
import net.antopfr.advancedweather.weather.WeatherManager;
import net.antopfr.advancedweather.util.BiomeMixinContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public class ServerLevelMixin {

    @Inject(method = "tickChunk", at = @At("HEAD"))
    private void onTickChunkHead(LevelChunk chunk, int randomTickSpeed, CallbackInfo ci) {
        BiomeMixinContext.setCurrentLevel((ServerLevel) (Object) this);
    }

    @Inject(method = "tickChunk", at = @At("RETURN"))
    private void onTickChunkReturn(CallbackInfo ci) {
        BiomeMixinContext.clearCurrentLevel();
    }

    @Inject(method = "tickPrecipitation", at = @At("HEAD"), cancellable = true)
    private void onTickPrecipitation(BlockPos blockPos, CallbackInfo ci) {
        ServerLevel level = (ServerLevel) (Object) this;
        WeatherTypes type = WeatherManager.get(level).getCurrentWeather(level);
        Biome.Precipitation falling = type.precipitation();

        if (falling == Biome.Precipitation.NONE) return;

        ci.cancel();

        BlockPos topPos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, blockPos);
        BlockPos belowPos = topPos.below();

        if (level.isAreaLoaded(belowPos, 1)
                && advancedweather$canFreezeAt(level, belowPos)
                && LocalAtmosphere.getLocalTemperature(level, belowPos) <= 0f) {
            level.setBlockAndUpdate(belowPos, Blocks.ICE.defaultBlockState());
        }

        if (falling == Biome.Precipitation.SNOW) {
            int maxLayers = level.getGameRules().getInt(GameRules.RULE_SNOW_ACCUMULATION_HEIGHT);
            if (maxLayers > 0 && level.random.nextFloat() < SnowMelt.layerChance(type)) {
                BlockState topState = level.getBlockState(topPos);
                boolean validSupport = Blocks.SNOW.defaultBlockState().canSurvive(level, topPos);

                if (topState.is(Blocks.SNOW)) {
                    int layers = topState.getValue(SnowLayerBlock.LAYERS);
                    int most = level.getBlockState(belowPos).is(BlockTags.ICE) ? 1 : Math.min(maxLayers, 8);
                    if (layers < most) {
                        BlockState newState = topState.setValue(SnowLayerBlock.LAYERS, layers + 1);
                        Block.pushEntitiesUp(topState, newState, level, topPos);
                        level.setBlockAndUpdate(topPos, newState);
                    }
                } else if (topState.isAir() && validSupport) {
                    level.setBlockAndUpdate(topPos, Blocks.SNOW.defaultBlockState());
                }
            }
        }

        BlockState belowState = level.getBlockState(belowPos);
        belowState.getBlock().handlePrecipitation(belowState, level, belowPos, falling);
    }

    @Unique
    private static boolean advancedweather$canFreezeAt(ServerLevel level, BlockPos water) {
        if (water.getY() < level.getMinBuildHeight() || water.getY() >= level.getMaxBuildHeight()) {
            return false;
        }
        if (level.getBrightness(LightLayer.BLOCK, water) >= 10) {
            return false;
        }

        BlockState state = level.getBlockState(water);
        FluidState fluid = level.getFluidState(water);
        if (fluid.getType() != Fluids.WATER || !(state.getBlock() instanceof LiquidBlock)) {
            return false;
        }

        return !(level.isWaterAt(water.west())
                && level.isWaterAt(water.east())
                && level.isWaterAt(water.north())
                && level.isWaterAt(water.south()));
    }
}
