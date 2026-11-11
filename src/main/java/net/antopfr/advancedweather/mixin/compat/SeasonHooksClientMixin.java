package net.antopfr.advancedweather.mixin.compat;

import net.antopfr.advancedweather.client.state.ClientWeatherState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import sereneseasons.season.SeasonHooks;

@Pseudo
@Mixin(value = SeasonHooks.class, remap = false)
public class SeasonHooksClientMixin {

    @Inject(method = "getPrecipitationAtSeasonal", at = @At("RETURN"), cancellable = true, remap = false)
    private static void aw_overridePrecipitationSeasonal(Level level, Holder<Biome> biome, BlockPos pos,
                                                         CallbackInfoReturnable<Biome.Precipitation> cir) {
        if (!level.isClientSide) return;

        cir.setReturnValue(ClientWeatherState.precipitating().precipitation());
    }

    @Inject(method = "hasPrecipitationSeasonal", at = @At("RETURN"), cancellable = true, remap = false)
    private static void aw_overrideHasPrecipitation(Level level, Holder<Biome> biome,
                                                    CallbackInfoReturnable<Boolean> cir) {
        if (!level.isClientSide) return;

        cir.setReturnValue(ClientWeatherState.precipitating().precipitation() != Biome.Precipitation.NONE);
    }
}
