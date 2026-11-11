package net.antopfr.advancedweather.mixin.compat;

import net.antopfr.advancedweather.compat.eclipticseasons.EclipticSeasonsCompat;
import net.antopfr.advancedweather.util.BiomeMixinContext;
import net.antopfr.advancedweather.weather.ServerRain;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = {
        "com.teamtea.eclipticseasons.api.util.EclipticUtil",
        "com.teamtea.eclipticseasons.common.core.biome.WeatherManager"
}, remap = false)
public abstract class EclipticSeasonsServerMixin {

    @Unique
    private static final String ASKED = "(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/biome/Biome;"
            + "Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/biome/Biome$Precipitation;";

    @Inject(method = "useSolarWeather()Z", at = @At("RETURN"), cancellable = true, require = 0)
    private static void advancedweather$ourWeather(CallbackInfoReturnable<Boolean> cir) {
        EclipticSeasonsCompat.report(2, "weather engine");
        cir.setReturnValue(false);
    }

    @Inject(method = {"getRainOrSnow" + ASKED, "getPrecipitationAt" + ASKED}, at = @At("RETURN"), cancellable = true, require = 0)
    private static void advancedweather$serverPrecipitation(Level level, Biome biome, BlockPos pos,
                                                            CallbackInfoReturnable<Biome.Precipitation> cir) {
        Level asked = level != null ? level : BiomeMixinContext.getCurrentLevel();
        if (!(asked instanceof ServerLevel)) return;

        Biome.Precipitation ours = EclipticSeasonsCompat.precipitation(ServerRain.weatherOf(asked));
        if (ours != null) {
            EclipticSeasonsCompat.report(4, "rain or snow");
            cir.setReturnValue(ours);
        }
    }
}
