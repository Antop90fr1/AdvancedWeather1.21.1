package net.antopfr.advancedweather.compat.coldsweat;

import net.antopfr.advancedweather.config.AWCommonConfig;
import net.antopfr.advancedweather.util.ModLoaded;
import net.antopfr.advancedweather.util.PlayerChecks;
import net.antopfr.advancedweather.weather.AtmosphericSystem;
import net.antopfr.advancedweather.weather.FeelsLikeCalculation;
import net.antopfr.advancedweather.weather.LocalAtmosphere;
import net.antopfr.advancedweather.weather.WeatherDefinitions;
import net.antopfr.advancedweather.weather.WeatherManager;
import net.antopfr.advancedweather.weather.WeatherTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;

public final class ColdSweatCompat {
    private ColdSweatCompat() {}

    private static final float SHELTERED_FACTOR = 0.35f;

    private static final float WIND_CHILL_FACTOR = 0.5f;

    private static final float MAX_DELTA_C = 30f;

    public static void register() {
        if (!ModLoaded.isColdSweatLoaded()) return;
        ColdSweatBridge.register();
    }

    public static float weatherDeltaCelsius(ServerLevel level, BlockPos pos) {
        AWCommonConfig config = AWCommonConfig.get();

        WeatherManager manager = WeatherManager.get(level);
        AtmosphericSystem atmosphere = manager.getAtmosphere(level);
        WeatherTypes current = manager.getCurrentWeather(level);

        float baseline = WeatherDefinitions.get(clearOf(current.dimension())).tempBase;
        float delta = atmosphere.getWeatherTemperature() - baseline;

        boolean sheltered = PlayerChecks.isShielded(level, pos);

        if (config.coldSweatWindChill && !sheltered) {
            float localTemp = LocalAtmosphere.getLocalTemperature(level, pos);
            float feelsLike = FeelsLikeCalculation.calculate(
                    localTemp,
                    LocalAtmosphere.getWindKmh(level, pos),
                    LocalAtmosphere.getLocalHumidity(level, pos));
            delta += (feelsLike - localTemp) * WIND_CHILL_FACTOR;
        }

        if (sheltered) {
            delta *= SHELTERED_FACTOR;
        }

        delta *= (float) config.coldSweatWeatherStrength;
        return Mth.clamp(delta, -MAX_DELTA_C, MAX_DELTA_C);
    }

    private static WeatherTypes clearOf(WeatherTypes.Dimension dimension) {
        return switch (dimension) {
            case NETHER -> WeatherTypes.NETHER_CLEAR;
            case END -> WeatherTypes.END_CLEAR;
            default -> WeatherTypes.CLEAR;
        };
    }
}
