package net.antopfr.advancedweather.mixin;

import net.antopfr.advancedweather.server.event.CropGrowingEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CropBlock.class)
public class CropBlockMixin {

    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    private void advancedweather$killedByIce(BlockState state, ServerLevel level, BlockPos pos, RandomSource random,
                                            CallbackInfo ci) {
        if (CropGrowingEvent.killedByIce(level, pos, random)) {
            level.destroyBlock(pos, false);
            ci.cancel();
        }
    }
}
