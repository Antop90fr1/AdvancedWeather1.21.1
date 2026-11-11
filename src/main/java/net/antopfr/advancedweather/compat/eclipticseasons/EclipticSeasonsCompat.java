package net.antopfr.advancedweather.compat.eclipticseasons;

import net.antopfr.advancedweather.util.AWLogger;
import net.antopfr.advancedweather.weather.WeatherManager;
import net.antopfr.advancedweather.weather.WeatherTypes;
import net.antopfr.advancedweather.weather.wind.WindField;
import net.antopfr.advancedweather.weather.wind.WindShelter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class EclipticSeasonsCompat {

    private EclipticSeasonsCompat() {}

    private static final int TERMS = 24;

    private static Object api;
    private static Method seasonEnabled;
    private static Method solarTerm;
    private static Method dayInTerm;
    private static Method daysPerTerm;
    private static boolean failed;
    private static int reported;

    public static float yearProgress(Level level) {
        if (failed) return -1f;
        try {
            if (api == null && !bind()) return -1f;
            if (!(Boolean) seasonEnabled.invoke(api, level)) return -1f;

            int term = ((Enum<?>) solarTerm.invoke(api, level)).ordinal();
            if (term >= TERMS) return -1f;

            int days = Math.max(1, (Integer) daysPerTerm.invoke(api, level));
            int day = Mth.clamp((Integer) dayInTerm.invoke(api, level), 0, days - 1);
            return (term + (day + 0.5f) / days) / TERMS;
        } catch (ReflectiveOperationException | RuntimeException e) {
            failed = true;
            AWLogger.AW.warn("[AdvancedWeather] Ecliptic Seasons: its seasons could not be read, weather will ignore them: {}", e.toString());
            return -1f;
        }
    }

    private static boolean bind() throws ReflectiveOperationException {
        Class<?> type = Class.forName("com.teamtea.eclipticseasons.api.EclipticSeasonsApi");
        Object instance = type.getMethod("getInstance").invoke(null);
        if (instance == null) return false;

        seasonEnabled = type.getMethod("isSeasonEnabled", Level.class);
        solarTerm = type.getMethod("getSolarTerm", Level.class);
        dayInTerm = type.getMethod("getDayInTerm", Level.class);
        daysPerTerm = type.getMethod("getLastingDaysOfEachTerm", Level.class);
        api = instance;
        report(1, "seasons");
        return true;
    }

    public static Biome.Precipitation precipitation(WeatherTypes weather) {
        if (weather == null) return null;
        return switch (weather) {
            case SNOW, BLIZZARD -> Biome.Precipitation.SNOW;
            case DRIZZLE, LIGHT_RAIN, HEAVY_RAIN, FREEZING_RAIN, THUNDERSTORM, HAIL -> Biome.Precipitation.RAIN;
            default -> null;
        };
    }

    private static final float CHIMES_START_KMH = 9f;
    private static final float CHIMES_STOP_KMH = 6f;

    public static boolean ringsChimes(Level level, BlockPos pos, boolean ringing) {
        report(8, "wind chimes");
        float felt = WindField.sample(level, pos).speedKmh() * WindShelter.exposure(level, pos);
        return felt >= (ringing ? CHIMES_STOP_KMH : CHIMES_START_KMH);
    }

    private static final float DEPTH_SCALE = 100f;
    private static final int UNANSWERED_LIMIT = 8;

    private static ServerLevel snowUpdateLevel;
    private static boolean depthOurs;
    private static int unanswered;
    private static Field biomeOfWeather;
    private static boolean snowFailed;

    public static void enterSnowUpdate(ServerLevel level) {
        snowUpdateLevel = level;
    }

    public static void exitSnowUpdate() {
        snowUpdateLevel = null;
    }

    public static Object snowStatus(Object theirs) {
        if (snowUpdateLevel == null || snowFailed || !(theirs instanceof Enum<?> verdict)) return null;
        if (!verdict.name().equals("NONE")) return null;
        if (!depthOurs && unanswered++ >= UNANSWERED_LIMIT) return null;

        for (Object other : verdict.getDeclaringClass().getEnumConstants()) {
            if (((Enum<?>) other).name().equals("SNOW_MELT")) return other;
        }
        return null;
    }

    public static float snowDepth(Object biomeWeather, float theirs) {
        ServerLevel level = snowUpdateLevel;
        if (level == null || snowFailed) return theirs;
        try {
            if (biomeOfWeather == null) biomeOfWeather = biomeWeather.getClass().getField("biomeHolder");
            if (!(biomeOfWeather.get(biomeWeather) instanceof Holder<?> biome)) return theirs;

            ResourceLocation id = biome.unwrapKey().map(ResourceKey::location).orElse(null);
            depthOurs = true;
            report(16, "snow depth");
            return DEPTH_SCALE * WeatherManager.get(level).getSnowPack(level).snow(id);
        } catch (ReflectiveOperationException | RuntimeException e) {
            snowFailed = true;
            AWLogger.AW.warn("[AdvancedWeather] Ecliptic Seasons: its snow could not be tied to ours, it keeps its own: {}", e.toString());
            return theirs;
        }
    }

    public static void report(int hook, String name) {
        if ((reported & hook) != 0) return;
        reported |= hook;
        AWLogger.AW.info("[AdvancedWeather] Ecliptic Seasons hook active: {}", name);
    }
}
