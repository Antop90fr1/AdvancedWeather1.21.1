package net.antopfr.advancedweather.mixin;

import net.antopfr.advancedweather.server.event.FogCover;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Monster.class)
public class MonsterMixin {

    @Inject(method = "isDarkEnoughToSpawn", at = @At("RETURN"), cancellable = true)
    private static void advancedweather$denseFog(ServerLevelAccessor level, BlockPos pos, RandomSource random,
                                                CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && FogCover.spawnsInFog(level, pos, random)) cir.setReturnValue(true);
    }
}
