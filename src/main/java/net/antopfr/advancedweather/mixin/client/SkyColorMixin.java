package net.antopfr.advancedweather.mixin.client;

import net.antopfr.advancedweather.client.state.ClientWeatherState;
import net.antopfr.advancedweather.compat.atmospherics.AtmosphericsCompat;
import net.antopfr.advancedweather.util.SkyMixinContext;
import net.antopfr.advancedweather.weather.WeatherDefinitions;
import net.antopfr.advancedweather.weather.WeatherTypes;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ClientLevel.class, priority = 1100)
public class SkyColorMixin {

    @Inject(method = "getSkyColor", at = @At("HEAD"))
    private void aw_beginSkyColor(Vec3 cameraPos, float partialTick, CallbackInfoReturnable<Vec3> cir) {
        AtmosphericsCompat.beginSkyColor();
    }

    @Inject(method = "getSkyColor", at = @At("RETURN"), cancellable = true)
    private void aw_overrideSkyColor(Vec3 cameraPos, float partialTick,
                                     CallbackInfoReturnable<Vec3> cir) {
        AtmosphericsCompat.endSkyColor();
        Vec3 vanilla = cir.getReturnValue();
        SkyMixinContext.setVanillaSkyColor(vanilla);

        WeatherTypes current = ClientWeatherState.getCurrentWeather();
        WeatherTypes previous = ClientWeatherState.getPreviousWeather();

        Vec3 to   = WeatherDefinitions.get(current).cloudTint;
        Vec3 from = WeatherDefinitions.get(previous).cloudTint;

        float t = ClientWeatherState.getSmoothedTransitionProgress(partialTick);
        Vec3 mix = from.lerp(to, t);

        Vec3 tinted = new Vec3(vanilla.x * mix.x, vanilla.y * mix.y, vanilla.z * mix.z);

        float ds = Mth.lerp(t,
                WeatherDefinitions.get(previous).skyDesaturation,
                WeatherDefinitions.get(current).skyDesaturation);

        double lum = (tinted.x + tinted.y + tinted.z) / 3.0;
        cir.setReturnValue(new Vec3(
                Mth.lerp(ds, tinted.x, lum),
                Mth.lerp(ds, tinted.y, lum),
                Mth.lerp(ds, tinted.z, lum)));
    }
}
