package net.antopfr.advancedweather.weather;

import net.antopfr.advancedweather.weather.wind.WindField;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

public class LocalAtmosphere {

    private static final float LAPSE_RATE_PRESSURE = 0.12f;
    private static final float LAPSE_RATE_TEMP     = 0.05f;

    public static float temperatureShift(Level level, ResourceLocation biome, int y) {
        return BiomeAtmosphereData.getBiomeOffset(level, biome) - (y - 64f) * LAPSE_RATE_TEMP;
    }

    public static float pressureShift(int y) {
        return -(y - 64f) * LAPSE_RATE_PRESSURE;
    }

    public static float humidityShift(Level level, ResourceLocation biome) {
        return BiomeAtmosphereData.getHumidityOffset(level, biome);
    }

    public static float getLocalTemperature(ServerLevel level, BlockPos pos) {
        WeatherManager manager = WeatherManager.get(level);
        AtmosphericSystem atmosphere = manager.getAtmosphere(level);
        DimensionProfile profile = DimensionProfile.of(level);

        float base = atmosphere.getTemperature();

        ResourceLocation biome = level.getBiome(pos).unwrapKey()
                .map(ResourceKey::location).orElse(null);
        return Mth.clamp(base + temperatureShift(level, biome, pos.getY()),
                profile.tMin - 40f, profile.tMax + 40f);
    }

    public static float getLocalPressure(ServerLevel level, BlockPos pos) {
        DimensionProfile profile = DimensionProfile.of(level);
        float base = WeatherManager.get(level).getAtmosphere(level).getPressure();

        float margin = (profile.pMax - profile.pMin) * 1.5f + 50f;
        return Mth.clamp(base + pressureShift(pos.getY()),
                Math.max(0f, profile.pMin - margin), profile.pMax + margin);
    }

    public static float getLocalHumidity(ServerLevel level, BlockPos pos) {
        WeatherManager manager = WeatherManager.get(level);
        float base = manager.getAtmosphere(level).getHumidity();

        ResourceLocation biome = level.getBiome(pos).unwrapKey()
                .map(ResourceKey::location).orElse(null);
        return Mth.clamp(base + humidityShift(level, biome), 0f, 100f);
    }

    public static float getWindKmh(ServerLevel level) {
        return WindField.baseSpeedKmh(level);
    }

    public static float getWindKmh(ServerLevel level, BlockPos pos) {
        return WindField.sample(level, pos).speedKmh();
    }
}
