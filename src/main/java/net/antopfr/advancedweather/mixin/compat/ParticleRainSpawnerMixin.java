package net.antopfr.advancedweather.mixin.compat;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.antopfr.advancedweather.compat.particlerain.ParticleRainCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "pigcart.particlerain.ParticleSpawner", remap = false)
public abstract class ParticleRainSpawnerMixin {

    @Unique
    private static final String TICK_BLOCK_FX = "tickBlockFX(Lnet/minecraft/core/BlockPos$MutableBlockPos;"
            + "Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/util/RandomSource;)V";

    @Inject(method = TICK_BLOCK_FX, at = @At("HEAD"), require = 0)
    private static void advancedweather$enter(CallbackInfo ci) {
        ParticleRainCompat.enter();
    }

    @Inject(method = TICK_BLOCK_FX, at = @At("RETURN"), require = 0)
    private static void advancedweather$exit(CallbackInfo ci) {
        ParticleRainCompat.exit();
    }

    @WrapOperation(
            method = {
                    "tickSkyFX(Lnet/minecraft/client/multiplayer/ClientLevel;Lnet/minecraft/world/phys/Vec3;)V",
                    "getSkyParticleDensity(Lnet/minecraft/client/multiplayer/ClientLevel;F)I"
            },
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;lerpInt(FII)I"),
            require = 0
    )
    private static int advancedweather$density(float storm, int usual, int stormy, Operation<Integer> original) {
        return ParticleRainCompat.density(original.call(storm, usual, stormy));
    }
}
