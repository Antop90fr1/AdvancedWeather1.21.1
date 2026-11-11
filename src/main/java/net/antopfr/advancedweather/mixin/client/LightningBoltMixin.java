package net.antopfr.advancedweather.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.antopfr.advancedweather.config.AWClientConfig;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LightningBolt.class)
public abstract class LightningBoltMixin {

    @Unique
    private static final double SEEN_FROM = 256.0;

    @Unique
    private static final float THUNDER_REACH = 24f;

    @WrapOperation(
            method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;playLocalSound("
                    + "DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FFZ)V")
    )
    private void advancedweather$thunderTravels(Level level, double x, double y, double z, SoundEvent sound,
                                               SoundSource source, float volume, float pitch, boolean delayed,
                                               Operation<Void> original) {
        if (sound == SoundEvents.LIGHTNING_BOLT_THUNDER && AWClientConfig.get().distantStorms) {
            original.call(level, x, y, z, sound, source, THUNDER_REACH, pitch, true);
        } else {
            original.call(level, x, y, z, sound, source, volume, pitch, delayed);
        }
    }

    @Inject(method = "shouldRenderAtSqrDistance", at = @At("HEAD"), cancellable = true)
    private void advancedweather$seenFromAfar(double distance, CallbackInfoReturnable<Boolean> cir) {
        if (AWClientConfig.get().distantStorms) cir.setReturnValue(distance < SEEN_FROM * SEEN_FROM);
    }
}
