package net.antopfr.advancedweather.api;

import net.antopfr.advancedweather.weather.*;
import net.antopfr.advancedweather.weather.wind.WindField;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class AdvancedWeatherAPI {

    private AdvancedWeatherAPI() {}

    public static WeatherTypes getCurrentWeather(ServerLevel level) {
        return WeatherManager.get(level).getCurrentWeather(level);
    }

    public static @Nullable WeatherTypes getComingWeather(ServerLevel level) {
        return WeatherManager.get(level).getComingWeather(level);
    }

    public static int getTicksUntilNextWeather(ServerLevel level) {
        return WeatherManager.get(level).getTicksUntilNextWeather(level);
    }

    public static WeatherForecast getForecast(ServerLevel level) {
        return WeatherManager.get(level).getForecast(level);
    }

    public static List<WeatherTypes> getRecentHistory(ServerLevel level) {
        return WeatherManager.get(level).getRecentHistory(level);
    }

    public static boolean isRaining(ServerLevel level) {
        return getCurrentWeather(level).isVanillaRaining();
    }

    public static boolean isThundering(ServerLevel level) {
        return getCurrentWeather(level).isVanillaThundering();
    }

    public static AtmosphereSnapshot getAtmosphere(ServerLevel level) {
        WeatherManager manager = WeatherManager.get(level);
        AtmosphericSystem a = manager.getAtmosphere(level);
        return new AtmosphereSnapshot(
                a.getPressure(), a.getPressureVel(), a.getDewPoint(),
                a.getTemperature(), a.getHumidity(), manager.getWindIntensity(level),
                manager.getMode(level).name(), a.getCategory(level).name());
    }

    public static float getLocalTemperature(ServerLevel level, BlockPos pos) {
        return LocalAtmosphere.getLocalTemperature(level, pos);
    }

    public static float getLocalHumidity(ServerLevel level, BlockPos pos) {
        return LocalAtmosphere.getLocalHumidity(level, pos);
    }

    public static float getLocalPressure(ServerLevel level, BlockPos pos) {
        return LocalAtmosphere.getLocalPressure(level, pos);
    }

    public static Vec3 getWindVelocity(ServerLevel level, Vec3 pos) {
        return WindField.sample(level, pos).velocity();
    }

    public static float getWindSpeedKmh(ServerLevel level, Vec3 pos) {
        return WindField.sample(level, pos).speedKmh();
    }

    public static float getSnowCover(ServerLevel level, BlockPos pos) {
        return WeatherManager.get(level).getSnowPack(level).snowAt(level, pos);
    }

    public static float getSnowCrust(ServerLevel level) {
        return WeatherManager.get(level).getSnowPack(level).crust();
    }

    public static float getSandCover(ServerLevel level, BlockPos pos) {
        return WeatherManager.get(level).getSandDrift(level).lyingAt(level, pos).amount();
    }

    public static boolean setWeather(ServerLevel level, WeatherTypes type) {
        if (!WeatherManager.isCompatibleWithLevel(type, level)) return false;
        WeatherManager.get(level).setCurrentWeather(level, type);
        return true;
    }

    public static boolean approachWeather(ServerLevel level, WeatherTypes type) {
        WeatherManager manager = WeatherManager.get(level);
        if (!WeatherManager.isCompatibleWithLevel(type, level) || manager.followsRealWeather(level)) return false;
        manager.approach(level, type);
        return true;
    }

    public static void startAutoWeather(ServerLevel level) {
        WeatherManager.get(level).startAutoWeather(level);
    }

    public static void applyForcing(ServerLevel level, float pressurePush, float dewPointPush,
                                    float tempOffset, int durationTicks) {
        applyForcing(level, pressurePush, dewPointPush, tempOffset, durationTicks, AtmosphericForcing.Bias.NONE);
    }

    public static void applyForcing(ServerLevel level, float pressurePush, float dewPointPush,
                                    float tempOffset, int durationTicks, AtmosphericForcing.Bias bias) {
        WeatherManager.get(level).getAtmosphere(level)
                .applyForcing(pressurePush, dewPointPush, tempOffset, durationTicks, bias);
    }

    public static void seedWeather(ServerLevel level, float pressurePush, float dewPointPush,
                                   float tempOffset, int durationTicks, AtmosphericForcing.Bias bias) {
        WeatherManager.get(level).seed(level, bias, pressurePush, dewPointPush, tempOffset, durationTicks);
    }
}
