package net.antopfr.advancedweather.client.state;

import net.antopfr.advancedweather.client.ClientLocalHistory;
import net.antopfr.advancedweather.client.ClientWeatherHistory;
import net.antopfr.advancedweather.util.FogColorLerp;
import net.antopfr.advancedweather.util.FogLerp;
import net.antopfr.advancedweather.weather.BiomeFogData;
import net.antopfr.advancedweather.weather.WeatherDefinitions;
import net.antopfr.advancedweather.weather.WeatherEffects;
import net.antopfr.advancedweather.weather.WeatherTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import java.util.EnumSet;
import java.util.Set;

public class ClientWeatherState {

    private static WeatherTypes currentWeather  = WeatherTypes.CLEAR;
    private static WeatherTypes previousWeather = WeatherTypes.CLEAR;

    private static float transitionProgress = 1.0f;

    private static final int TRANSITION_DURATION = 20 * 10;
    private static int transitionTicksElapsed = 0;

    public static WeatherTypes getCurrentWeather() {
        return currentWeather;
    }

    public static WeatherTypes getPreviousWeather() {
        return previousWeather;
    }

    public static float getTransitionProgress() {
        return transitionProgress;
    }

    public static void setCurrentWeather(WeatherTypes type) {
        if (type == currentWeather) return;
        previousWeather = currentWeather;
        currentWeather  = type;
        transitionProgress     = 0.0f;
        transitionTicksElapsed = 0;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            refreshFogColor(mc);
            if (currentBiome != null) {
                ClientAtmosphereState.onBiomeChanged(currentBiome);
            }
        }
    }

    public static void tickTransition() {
        if (transitionProgress < 1.0f) {
            transitionTicksElapsed++;
            transitionProgress = Math.min(1.0f, (float)transitionTicksElapsed / TRANSITION_DURATION);
        }
        fogColorLerp.tick();
        ClientAtmosphereState.tick();
    }

    public static float getSmoothedTransitionProgress(float tickDelta) {
        if (transitionProgress >= 1.0f) return 1.0f;
        float smoothed = (transitionTicksElapsed + tickDelta) / TRANSITION_DURATION;
        return Math.min(1.0f, smoothed);
    }

    public static float presence(WeatherTypes type, float tickDelta) {
        float progress = getSmoothedTransitionProgress(tickDelta);
        if (currentWeather == type) return previousWeather == type ? 1f : progress;
        return previousWeather == type ? 1f - progress : 0f;
    }

    public static WeatherTypes precipitating() {
        if (WeatherDefinitions.get(currentWeather).rainIntensity > 0f) return currentWeather;
        if (transitionProgress < 1f && WeatherDefinitions.get(previousWeather).rainIntensity > 0f) return previousWeather;
        return currentWeather;
    }

    private static Set<WeatherEffects> activeEffects = EnumSet.noneOf(WeatherEffects.class);

    public static Set<WeatherEffects> getActiveEffects() { return activeEffects; }
    public static void setActiveEffects(Set<WeatherEffects> effects) { activeEffects = effects; }
    public static boolean hasEffect(WeatherEffects effect) { return activeEffects.contains(effect); }

    public static final FogColorLerp fogColorLerp = new FogColorLerp(20 * 10);

    public static ResourceLocation getCurrentBiome() {
        return currentBiome;
    }

    private static ResourceLocation currentBiome = null;

    public static void tickBiomeCheck(Minecraft mc) {
        if (mc.level == null || mc.player == null) return;
        ResourceLocation biome = mc.level.getBiome(mc.player.blockPosition())
                .unwrapKey().map(ResourceKey::location).orElse(null);
        if (biome != null && !biome.equals(currentBiome)) {
            currentBiome = biome;
            refreshFogColor(mc);
            ClientAtmosphereState.onBiomeChanged(biome);
        }
    }

    public static void refreshFogColor(Minecraft mc) {
        int target = BiomeFogData.getColor(mc.level, currentWeather, currentBiome);
        fogColorLerp.setTarget(target);
    }

    public static final FogLerp fogDistanceLerp = new FogLerp();

    public static void resetForDimensionChange() {
        currentWeather         = WeatherTypes.CLEAR;
        previousWeather        = WeatherTypes.CLEAR;
        transitionProgress     = 1.0f;
        transitionTicksElapsed = 0;
        fogColorLerp.reset(0xBFBFBF);
        currentBiome           = null;
        ClientAtmosphereState.resetOffsets();
        ClientLocalHistory.resetForDimensionChange();
    }

    public static void reset() {
        resetForDimensionChange();
        ClientWeatherHistory.clear();
        ClientLocalHistory.clear();
        ClientAtmosphereState.reset();
    }
}