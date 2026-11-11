package net.antopfr.advancedweather.client.render;

import net.antopfr.advancedweather.client.state.ClientWeatherState;
import net.antopfr.advancedweather.weather.WeatherDefinitions;
import net.minecraft.util.Mth;

public final class SkyVeil {

    private SkyVeil() {}

    public static float visibility(float partialTick) {
        float from = WeatherDefinitions.get(ClientWeatherState.getPreviousWeather()).hidesSun ? 0f : 1f;
        float to = WeatherDefinitions.get(ClientWeatherState.getCurrentWeather()).hidesSun ? 0f : 1f;
        return Mth.lerp(ClientWeatherState.getSmoothedTransitionProgress(partialTick), from, to);
    }
}
