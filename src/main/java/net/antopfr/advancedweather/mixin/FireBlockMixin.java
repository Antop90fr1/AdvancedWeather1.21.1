package net.antopfr.advancedweather.mixin;

import net.antopfr.advancedweather.config.AWCommonConfig;
import net.antopfr.advancedweather.weather.wind.WindField;
import net.antopfr.advancedweather.weather.wind.WindSample;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FireBlock.class)
public class FireBlockMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void advancedweather$windSpread(BlockState state, ServerLevel level, BlockPos pos,
                                            RandomSource random, CallbackInfo ci) {
        if (!AWCommonConfig.get().windSpreadsFire) return;

        WindSample wind = WindField.sample(level, pos);
        float intensity = wind.intensity();
        if (intensity < 0.4f) return;
        if (random.nextFloat() > intensity * 0.5f) return;

        Vec3 blow = wind.direction();
        Direction dir = Direction.getNearest(blow.x, 0.0, blow.z);
        BlockPos target = pos.relative(dir);
        if (!level.getBlockState(target).isAir()) return;

        for (Direction d : Direction.values()) {
            BlockPos adj = target.relative(d);
            if (level.getBlockState(adj).isFlammable(level, adj, d.getOpposite())) {
                level.setBlockAndUpdate(target, BaseFireBlock.getState(level, target));
                return;
            }
        }
    }
}
