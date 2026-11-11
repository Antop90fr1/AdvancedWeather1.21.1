package net.antopfr.advancedweather.mixin.compat;

import com.llamalad7.mixinextras.sugar.Local;
import net.antopfr.advancedweather.compat.eclipticseasons.EclipticSeasonsCompat;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Pseudo
@Mixin(targets = "com.teamtea.eclipticseasons.common.core.biome.WeatherManager", remap = false)
public abstract class EclipticSnowDepthMixin {

    @Unique
    private static final String UPDATE = "updateSnowOrMelt(Lnet/minecraft/server/level/ServerLevel;"
            + "Lcom/teamtea/eclipticseasons/common/core/biome/WeatherManager$BiomeWeather;"
            + "Lnet/minecraft/util/RandomSource;IZ)V";

    @Unique
    private static final String STATUS = "getSnowStatus(Lnet/minecraft/server/level/ServerLevel;"
            + "Lnet/minecraft/core/Holder;Lnet/minecraft/core/BlockPos;Z)"
            + "Lcom/teamtea/eclipticseasons/common/core/biome/WeatherManager$SnowRenderStatus;";

    @Inject(method = UPDATE, at = @At("HEAD"), require = 0)
    private static void advancedweather$enterSnowUpdate(CallbackInfo ci, @Local(argsOnly = true) ServerLevel level) {
        EclipticSeasonsCompat.enterSnowUpdate(level);
    }

    @Inject(method = UPDATE, at = @At("RETURN"), require = 0)
    private static void advancedweather$exitSnowUpdate(CallbackInfo ci) {
        EclipticSeasonsCompat.exitSnowUpdate();
    }

    @Inject(method = STATUS, at = @At("RETURN"), cancellable = true, require = 0)
    private static void advancedweather$alwaysWriteDepth(CallbackInfoReturnable<Object> cir) {
        Object writes = EclipticSeasonsCompat.snowStatus(cir.getReturnValue());
        if (writes != null) cir.setReturnValue(writes);
    }
}
