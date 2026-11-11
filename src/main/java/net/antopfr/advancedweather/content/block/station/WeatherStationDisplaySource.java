package net.antopfr.advancedweather.content.block.station;

import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import net.antopfr.advancedweather.weather.LocalAtmosphere;
import net.antopfr.advancedweather.weather.WeatherManager;
import net.antopfr.advancedweather.weather.WeatherTypes;
import net.antopfr.advancedweather.weather.wind.WindScale;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;

import java.util.List;

public class WeatherStationDisplaySource extends DisplaySource {

    @Override
    public List<MutableComponent> provideText(DisplayLinkContext context, DisplayTargetStats stats) {
        if (!(context.level() instanceof ServerLevel level)) return EMPTY;
        BlockPos pos = context.getSourcePos();

        WeatherTypes weather = WeatherManager.get(level).getCurrentWeather(level);
        float temp = LocalAtmosphere.getLocalTemperature(level, pos);
        float pressure = LocalAtmosphere.getLocalPressure(level, pos);
        float hum = LocalAtmosphere.getLocalHumidity(level, pos);
        float windKmh = LocalAtmosphere.getWindKmh(level);

        if (stats.maxRows() >= 4) {
            return List.of(
                    weather.displayName().copy(),
                    Component.literal(String.format("%.1f °C  %.0f%%", temp, hum)),
                    Component.literal(String.format("%.1f hPa", pressure)),
                    Component.literal(String.format("%.0f km/h ", windKmh)).append(WindScale.beaufort(windKmh))
            );
        }
        return List.of(weather.displayName().copy()
                .append(String.format(" %.0f°C %.0fkm/h", temp, windKmh)));
    }

    @Override
    protected String getTranslationKey() {
        return "weather_station";
    }
}
