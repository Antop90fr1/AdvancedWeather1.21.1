package net.antopfr.advancedweather.client.render;

import net.antopfr.advancedweather.client.state.ClientWeatherState;
import net.antopfr.advancedweather.weather.WeatherDefinitions;
import net.antopfr.advancedweather.weather.WeatherTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class SkyVeil {

    private SkyVeil() {}

    public static float visibility(float partialTick) {
        float from = WeatherDefinitions.get(ClientWeatherState.getPreviousWeather()).hidesSun ? 0f : 1f;
        float to = WeatherDefinitions.get(ClientWeatherState.getCurrentWeather()).hidesSun ? 0f : 1f;
        return Mth.lerp(ClientWeatherState.getSmoothedTransitionProgress(partialTick), from, to);
    }

    public static Vec3 tint(Vec3 color, float partialTick) {
        WeatherTypes current = ClientWeatherState.getCurrentWeather();
        WeatherTypes previous = ClientWeatherState.getPreviousWeather();
        float t = ClientWeatherState.getSmoothedTransitionProgress(partialTick);
        Vec3 mix = WeatherDefinitions.get(previous).cloudTint.lerp(WeatherDefinitions.get(current).cloudTint, t);
        Vec3 tinted = new Vec3(color.x * mix.x, color.y * mix.y, color.z * mix.z);
        float ds = Mth.lerp(t, WeatherDefinitions.get(previous).skyDesaturation, WeatherDefinitions.get(current).skyDesaturation);
        double lum = (tinted.x + tinted.y + tinted.z) / 3.0;
        return new Vec3(Mth.lerp(ds, tinted.x, lum), Mth.lerp(ds, tinted.y, lum), Mth.lerp(ds, tinted.z, lum));
    }
}
