package net.antopfr.advancedweather.compat.sereneseasons;

import net.antopfr.advancedweather.weather.WeatherTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;

public class SeasonModifiers {

    private static final int SPRING = 0;
    private static final int SUMMER = 1;
    private static final int AUTUMN = 2;
    private static final int WINTER = 3;

    private record Blend(int from, int to, float share) {}

    private static Blend blend(float year) {
        float position = year * 4f - 0.5f;
        int floor = Mth.floor(position);
        float t = position - floor;
        return new Blend(Math.floorMod(floor, 4), Math.floorMod(floor + 1, 4), t * t * (3f - 2f * t));
    }

    private static float between(float from, float to, float share) {
        return (float) Math.exp(Mth.lerp((double) share, Math.log(from), Math.log(to)));
    }

    // TRANSITION PROBABILITY
    public static float seasonModifier(WeatherTypes target, ServerLevel level) {
        float year = SeasonCompat.yearProgress(level);
        if (year < 0f) return 1.0f;

        Blend blend = blend(year);
        return between(seasonModifier(blend.from(), target), seasonModifier(blend.to(), target), blend.share());
    }

    private static float seasonModifier(int season, WeatherTypes target) {
        return switch (season) {
            case SPRING -> springModifier(target);
            case SUMMER -> summerModifier(target);
            case AUTUMN -> autumnModifier(target);
            default -> winterModifier(target);
        };
    }

    private static float springModifier(WeatherTypes type) {
        return switch (type) {
            case LIGHT_RAIN, DRIZZLE, OVERCAST -> 1.6f;
            case THUNDERSTORM, HEAVY_RAIN      -> 1.3f;
            case SUNNY, CLEAR                  -> 1.1f;
            case SNOW, BLIZZARD, FREEZING_RAIN  -> 0.3f;
            case SANDSTORM                      -> 0.5f;
            default -> 1.0f;
        };
    }

    private static float summerModifier(WeatherTypes type) {
        return switch (type) {
            case SUNNY, CLEAR              -> 1.6f;
            case THUNDERSTORM, HAIL        -> 2.5f;
            case SANDSTORM                 -> 1.4f;
            case WINDY                     -> 1.2f;
            case SNOW, BLIZZARD, FREEZING_RAIN -> 0.05f;
            case FOG, DENSE_FOG            -> 0.6f;
            default -> 1.0f;
        };
    }

    private static float autumnModifier(WeatherTypes type) {
        return switch (type) {
            case FOG, DENSE_FOG, MIST      -> 1.7f;
            case OVERCAST, CLOUDY          -> 1.4f;
            case LIGHT_RAIN, HEAVY_RAIN, DRIZZLE -> 1.4f;
            case WINDY                     -> 1.3f;
            case THUNDERSTORM              -> 1.1f;
            case SUNNY                     -> 0.7f;
            case SANDSTORM                 -> 0.3f;
            case SNOW, FREEZING_RAIN       -> 0.6f;
            case BLIZZARD                  -> 0.3f;
            default -> 1.0f;
        };
    }

    private static float winterModifier(WeatherTypes type) {
        return switch (type) {
            case SNOW, BLIZZARD            -> 1.8f;
            case FREEZING_RAIN, HAIL       -> 1.5f;
            case FOG, DENSE_FOG            -> 1.3f;
            case OVERCAST                  -> 1.3f;
            case SUNNY, SANDSTORM          -> 0.2f;
            case THUNDERSTORM              -> 0.4f;
            default -> 1.0f;
        };
    }

    private static final float[] TEMPERATURE = {-2f, 6f, -3f, -10f};
    private static final float[] DEW_POINT = {1f, 3f, 0f, -5f};

    public static float temperatureOffset(ServerLevel level) {
        return eased(TEMPERATURE, level);
    }

    public static float dewPointOffset(ServerLevel level) {
        return eased(DEW_POINT, level);
    }

    private static float eased(float[] midSeason, ServerLevel level) {
        float year = SeasonCompat.yearProgress(level);
        if (year < 0f) return 0f;

        Blend blend = blend(year);
        return Mth.lerp(blend.share(), midSeason[blend.from()], midSeason[blend.to()]);
    }

    public static float durationModifier(WeatherTypes type, ServerLevel level) {
        float year = SeasonCompat.yearProgress(level);
        if (year < 0f) return 1.0f;

        Blend blend = blend(year);
        return between(durationModifier(blend.from(), type), durationModifier(blend.to(), type), blend.share());
    }

    private static float durationModifier(int season, WeatherTypes type) {
        return switch (season) {
            case WINTER -> switch (type) {
                case SNOW, BLIZZARD -> 1.4f;
                case SUNNY, CLEAR   -> 0.7f;
                default -> 1.0f;
            };
            case SUMMER -> switch (type) {
                case SUNNY, CLEAR   -> 1.3f;
                case THUNDERSTORM   -> 0.8f;
                default -> 1.0f;
            };
            default -> 1.0f;
        };
    }

    public static boolean isSeasonalType(WeatherTypes t) {
        return switch (t) {
            case SNOW, BLIZZARD, FREEZING_RAIN -> true;
            case SANDSTORM, HAIL -> true;
            default -> false;
        };
    }
}