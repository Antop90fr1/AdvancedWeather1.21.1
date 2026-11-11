package net.antopfr.advancedweather.weather;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;

public final class SnowMelt {

    private SnowMelt() {}

    private static final float BLIZZARD_PACE = 2.5f;

    private static final float MELT_TICKS = 20 * 60 * 5;
    private static final float REFERENCE_C = 8f;

    private static final float FASTEST = 6f;
    public static final float CRUST_TICKS = 20 * 45;
    private static final float CRUST_HOLD = 2f;
    private static final float CRUST_WEAR = 0.5f;

    public static float falling(WeatherTypes weather) {
        return switch (weather) {
            case SNOW -> 1f;
            case BLIZZARD -> BLIZZARD_PACE;
            default -> 0f;
        };
    }

    public static float layerChance(WeatherTypes weather) {
        return falling(weather) / BLIZZARD_PACE;
    }

    public static float perTick(float temperatureC, float crust) {
        if (temperatureC <= 0f) return 0f;
        return Math.min(temperatureC / REFERENCE_C, FASTEST) / MELT_TICKS / (1f + CRUST_HOLD * crust);
    }

    public static float crustWear(float temperatureC) {
        return perTick(temperatureC, 0f) * CRUST_WEAR;
    }

    public static float perRandomTick(ServerLevel level, float temperatureC, float crust) {
        int speed = Math.max(1, level.getGameRules().getInt(GameRules.RULE_RANDOMTICKING));
        return perTick(temperatureC, crust) * 4096f / speed;
    }
}
