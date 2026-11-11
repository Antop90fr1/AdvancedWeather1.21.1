package net.antopfr.advancedweather.mixin.client;

import net.antopfr.advancedweather.client.state.ClientWeatherState;
import net.antopfr.advancedweather.compat.atmospherics.AtmosphericsCompat;
import net.antopfr.advancedweather.compat.particlerain.ParticleRainCompat;
import net.antopfr.advancedweather.weather.WeatherDefinitions;
import net.antopfr.advancedweather.weather.WeatherTypes;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(Level.class)
public class RainLevelMixin {
    @Inject(method = "getRainLevel", at = @At("HEAD"), cancellable = true)
    private void onGetRainLevel(float delta, CallbackInfoReturnable<Float> cir) {
        if (!((Level) (Object) this).isClientSide) return;

        if (ParticleRainCompat.asking()) {
            float seen = ParticleRainCompat.rainLevel(delta);
            if (!Float.isNaN(seen)) cir.setReturnValue(seen);
            return;
        }
        if (AtmosphericsCompat.asking()) {
            float seen = AtmosphericsCompat.rainLevel(delta);
            if (!Float.isNaN(seen)) {
                cir.setReturnValue(seen);
                return;
            }
        }

        WeatherTypes current = ClientWeatherState.getCurrentWeather();
        WeatherTypes previous = ClientWeatherState.getPreviousWeather();

        float from = WeatherDefinitions.get(previous).rainIntensity;
        float to   = WeatherDefinitions.get(current).rainIntensity;

        if (from < 0 && to < 0) return;

        float t        = ClientWeatherState.getSmoothedTransitionProgress(delta);
        float fromSafe = from < 0 ? 0f : from;
        float toSafe   = to   < 0 ? 0f : to;

        cir.setReturnValue(fromSafe + (toSafe - fromSafe) * t);
    }

    @Inject(method = "getThunderLevel", at = @At("HEAD"), cancellable = true)
    private void onGetThunderLevel(float delta, CallbackInfoReturnable<Float> cir) {
        if (ParticleRainCompat.asking()) {
            cir.setReturnValue(ParticleRainCompat.thunderLevel(delta));
        } else if (AtmosphericsCompat.asking()) {
            float seen = AtmosphericsCompat.thunderLevel(delta);
            if (!Float.isNaN(seen)) cir.setReturnValue(seen);
        }
    }
}
