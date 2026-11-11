package net.antopfr.advancedweather.weather;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ServerRain {

    private ServerRain() {}

    private static final Map<ResourceKey<Level>, WeatherTypes> LEVELS = new ConcurrentHashMap<>();

    public static void update(ServerLevel level, WeatherTypes weather) {
        LEVELS.put(level.dimension(), weather);
    }

    public static WeatherTypes weatherOf(Level level) {
        return LEVELS.get(level.dimension());
    }

    public static float of(Level level) {
        WeatherTypes weather = LEVELS.get(level.dimension());
        return weather == null ? -1f : WeatherDefinitions.get(weather).rainIntensity;
    }

    public static void clear() {
        LEVELS.clear();
    }
}
