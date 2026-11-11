package net.antopfr.advancedweather.mixin;

import net.antopfr.advancedweather.server.event.CropGrowingEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SaplingBlock.class)
public class SaplingBlockMixin {

    @Inject(method = "randomTick", at = @At("TAIL"))
    private void advancedweather$rainBringsOn(BlockState state, ServerLevel level, BlockPos pos, RandomSource random,
                                             CallbackInfo ci) {
        float extra = CropGrowingEvent.saplingGrowth(level, pos);
        if (extra <= 0f || random.nextFloat() >= extra) return;

        BlockState now = level.getBlockState(pos);
        if (now.getBlock() != (Object) this || level.getMaxLocalRawBrightness(pos.above()) < 9) return;
        ((SaplingBlock) (Object) this).advanceTree(level, pos, now, random);
    }
}
