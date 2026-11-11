package net.antopfr.advancedweather.mixin.compat;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "sereneseasons.season.RandomUpdateHandler", remap = false)
public abstract class SereneMeltMixin {

    @Inject(
            method = "meltInChunk(Lnet/minecraft/server/level/ChunkMap;Lnet/minecraft/world/level/chunk/LevelChunk;F)V",
            at = @At("HEAD"), cancellable = true, require = 0
    )
    private static void advancedweather$ourMelt(CallbackInfo ci) {
        ci.cancel();
    }
}
