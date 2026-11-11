package net.antopfr.advancedweather.mixin.client;

import net.minecraft.client.renderer.DimensionSpecialEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DimensionSpecialEffects.NetherEffects.class)
public class NetherFogMixin {

    @Inject(
            method = "isFoggyAt",
            at = @At("HEAD"),
            cancellable = true
    )
    private void noNetherFog(int x, int z, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }
}