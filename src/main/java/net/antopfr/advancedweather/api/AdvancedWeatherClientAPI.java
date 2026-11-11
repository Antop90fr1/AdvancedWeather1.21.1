package net.antopfr.advancedweather.api;

import net.antopfr.advancedweather.client.frost.FrozenGround;
import net.antopfr.advancedweather.client.frost.GroundCover;
import net.antopfr.advancedweather.client.state.ClientAtmosphereState;
import net.antopfr.advancedweather.client.state.ClientWeatherState;
import net.antopfr.advancedweather.client.storm.ComingWeather;
import net.antopfr.advancedweather.client.wind.ClientWind;
import net.minecraft.world.phys.Vec3;
import net.antopfr.advancedweather.weather.WeatherTypes;
import org.jetbrains.annotations.Nullable;

public final class AdvancedWeatherClientAPI {

    private AdvancedWeatherClientAPI() {}

    public static WeatherTypes getCurrentWeather() {
        return ClientWeatherState.getCurrentWeather();
    }

    public static WeatherTypes getPreviousWeather() {
        return ClientWeatherState.getPreviousWeather();
    }

    public static float getTransitionProgress() {
        return ClientWeatherState.getTransitionProgress();
    }

    public static @Nullable WeatherTypes getComingWeather() {
        return getTicksUntilArrival() >= 0 ? ComingWeather.weather() : null;
    }

    public static long getTicksUntilArrival() {
        return ComingWeather.ticksAway(ComingWeather.weather());
    }

    public static float getPressure() {
        return ClientAtmosphereState.getPressure();
    }

    public static float getPressureTrend() {
        return ClientAtmosphereState.getTrend();
    }

    public static String getPressureCategory() {
        return ClientAtmosphereState.getCategory();
    }

    public static float getWindIntensity() {
        return ClientAtmosphereState.getWindIntensity();
    }

    public static float getLocalWindSpeedKmh() {
        return ClientWind.localKmh();
    }

    public static Vec3 getWindDirection() {
        return ClientWind.direction();
    }

    public static float getTemperature() {
        return ClientAtmosphereState.getLocalTemperature();
    }

    public static float getHumidity() {
        return ClientAtmosphereState.getLocalHumidity();
    }

    public static float getSnowCover() {
        return GroundCover.snow(1f);
    }

    public static float getSandCover() {
        return GroundCover.sand(1f);
    }

    public static float getIceCover() {
        return FrozenGround.ice();
    }

    public static @Nullable WeatherTypes getPredictedNext() {
        return weatherNamed(ClientAtmosphereState.getPredictedNext());
    }

    public static @Nullable WeatherTypes getPredictedIn30Min() {
        return weatherNamed(ClientAtmosphereState.getPredictedIn30min());
    }

    public static String getMode() {
        return ClientAtmosphereState.getMode();
    }

    private static @Nullable WeatherTypes weatherNamed(String name) {
        try {
            return WeatherTypes.valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
