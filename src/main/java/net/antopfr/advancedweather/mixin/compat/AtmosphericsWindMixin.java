package net.antopfr.advancedweather.mixin.compat;

import net.antopfr.advancedweather.compat.atmospherics.AtmosphericsCompat;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.beash.atmospherics.particle.DryWindField", remap = false)
public abstract class AtmosphericsWindMixin {

    @Inject(
            method = "wind(Lnet/minecraft/client/multiplayer/ClientLevel;DDDF)Lnet/minecraft/world/phys/Vec3;",
            at = @At("RETURN"), cancellable = true, require = 0
    )
    private static void advancedweather$wind(ClientLevel level, double x, double y, double z, float rain,
                                             CallbackInfoReturnable<Vec3> cir) {
        Vec3 ours = AtmosphericsCompat.wind(level, x, y, z, cir.getReturnValue());
        if (ours != null) cir.setReturnValue(ours);
    }
}
