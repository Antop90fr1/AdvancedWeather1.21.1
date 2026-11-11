package net.antopfr.advancedweather.client.state;

import net.antopfr.advancedweather.weather.BiomeAtmosphereData;
import net.antopfr.advancedweather.weather.DimensionProfile;
import net.antopfr.advancedweather.weather.wind.WindField;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

public class ClientAtmosphereState {

    private static float pressure          = 1013f;
    private static float pressureVel       = 0f;
    private static float pressureIn30      = 1013f;
    private static String category         = "NEUTRAL";
    private static String predictedNext    = "-";
    private static String predictedIn30min = "-";
    private static String mode             = "PROCEDURAL";
    private static float confidenceNext = 50f;
    private static float confidenceIn30 = 50f;

    private static float temperature       = 15f;
    private static float tempForecast      = 15f;
    private static float humidity          = 60f;
    private static float humForecast       = 60f;

    private static float tempOffset        = 0f;
    private static float tempOffsetTarget  = 0f;
    private static float humOffset         = 0f;
    private static float humOffsetTarget   = 0f;

    private static final float LAPSE_RATE_PRESSURE = 0.12f;
    private static final float LAPSE_RATE_TEMP     = 0.05f;

    // NETWORK UPDATES
    public static void updatePressure(float p, float vel, float f30, String cat, String next, String in30,
                                      String m, float confNext, float confIn30) {
        pressure = p;
        pressureVel = vel;
        pressureIn30 = f30;
        category = cat;
        predictedNext = next;
        predictedIn30min = in30;
        mode = m;
        confidenceNext = confNext;
        confidenceIn30 = confIn30;
    }

    public static void updateTemperature(float t, float f) {
        temperature = t;
        tempForecast = f;
    }

    public static void updateHumidity(float h, float f) {
        humidity = h;
        humForecast = f;
    }

    // BIOMES
    public static void onBiomeChanged(ResourceLocation biome) {
        Level level = Minecraft.getInstance().level;
        if (biome == null) {
            tempOffsetTarget = 0f;
            humOffsetTarget = 0f;
            return;
        }
        tempOffsetTarget = BiomeAtmosphereData.getBiomeOffset(level, biome);
        humOffsetTarget  = BiomeAtmosphereData.getHumidityOffset(level, biome);
    }

    public static void tick() {
        tempOffset = Mth.lerp(0.02f, tempOffset, tempOffsetTarget);
        humOffset  = Mth.lerp(0.02f, humOffset, humOffsetTarget);
    }

    // DIMENSION PROFILE

    private static DimensionProfile profile() {
        Level level = Minecraft.getInstance().level;
        return level != null ? DimensionProfile.of(level) : DimensionProfile.FALLBACK;
    }

    private static DimensionProfile profile(Level level) {
        return level != null ? DimensionProfile.of(level) : DimensionProfile.FALLBACK;
    }

    private static float clampPressure(float value, DimensionProfile p) {
        float margin = (p.pMax - p.pMin) * 1.5f + 50f;
        return Mth.clamp(value, Math.max(0f, p.pMin - margin), p.pMax + margin);
    }

    private static float clampTemperature(float value, DimensionProfile p) {
        return Mth.clamp(value, p.tMin - 40f, p.tMax + 40f);
    }

    // GETTERS
    private static float getClientPlayerY() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null ? (float) mc.player.getY() : 64f;
    }

    public static float getPressure() { return pressure; }

    public static float getLocalPressure() {
        float deltaY = getClientPlayerY() - 64f;
        return clampPressure(pressure - (deltaY * LAPSE_RATE_PRESSURE), profile());
    }

    public static float getTrend()             { return pressureVel; }
    public static float getForecast30()        { return pressureIn30; }
    public static String getCategory()         { return category; }
    public static String getPredictedNext()    { return predictedNext; }
    public static String getPredictedIn30min() { return predictedIn30min; }
    public static float getWindIntensity()     { return WindField.clientState().intensity(); }
    public static String getMode()             { return mode; }
    public static float getConfidenceNext() { return confidenceNext; }
    public static float getConfidenceIn30() { return confidenceIn30; }

    public static float getLocalTemperature() {
        float deltaY = getClientPlayerY() - 64f;
        float localT = (temperature + tempOffset) - (deltaY * LAPSE_RATE_TEMP);
        return clampTemperature(localT, profile());
    }

    public static float getLocalTemperatureForecast() {
        float deltaY = getClientPlayerY() - 64f;
        float localTForecast = (tempForecast + tempOffsetTarget) - (deltaY * LAPSE_RATE_TEMP);
        return clampTemperature(localTForecast, profile());
    }

    public static float getLocalHumidity()          { return Mth.clamp(humidity + humOffset, 0f, 100f); }
    public static float getLocalHumidityForecast()  { return Mth.clamp(humForecast + humOffsetTarget, 0f, 100f); }
    public static float getTempOffset() { return tempOffset; }

    public static float getLocalPressureAt(Level level, BlockPos pos) {
        float deltaY = pos.getY() - 64f;
        return clampPressure(pressure - (deltaY * LAPSE_RATE_PRESSURE), profile(level));
    }

    public static float getLocalTemperatureAt(Level level, BlockPos pos) {
        float biomeOffset = 0f;
        var biomeKey = level.getBiome(pos).unwrapKey().map(ResourceKey::location).orElse(null);
        if (biomeKey != null) {
            biomeOffset = BiomeAtmosphereData.getBiomeOffset(level, biomeKey);
        }
        float deltaY = pos.getY() - 64f;
        float localT = (temperature + biomeOffset) - (deltaY * LAPSE_RATE_TEMP);
        return clampTemperature(localT, profile(level));
    }

    public static float getLocalHumidityAt(Level level, BlockPos pos) {
        var biomeKey = level.getBiome(pos).unwrapKey().map(ResourceKey::location).orElse(null);
        float biomeOffset = BiomeAtmosphereData.getHumidityOffset(level, biomeKey);
        return Mth.clamp(humidity + biomeOffset, 0f, 100f);
    }

    // RESET

    public static void resetOffsets() {
        tempOffset        = 0f;
        tempOffsetTarget  = 0f;
        humOffset         = 0f;
        humOffsetTarget   = 0f;
    }

    public static void reset() {
        pressure = 1013f; pressureVel = 0f; pressureIn30 = 1013f;
        category = "NEUTRAL"; predictedNext = "-"; predictedIn30min = "-";
        WindField.clientState().reset(); mode = "PROCEDURAL";
        confidenceNext = 50f; confidenceIn30 = 50f;
        temperature = 15f; tempForecast = 15f;
        humidity = 60f; humForecast = 60f;
        tempOffset = 0f; tempOffsetTarget = 0f;
        humOffset = 0f; humOffsetTarget = 0f;
    }
}