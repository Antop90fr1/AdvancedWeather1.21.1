package net.antopfr.advancedweather.api.event;

import net.antopfr.advancedweather.weather.WeatherTypes;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.Event;

public class WeatherChangeEvent extends Event {

    private final ServerLevel level;
    private final WeatherTypes.Dimension dimension;
    private final WeatherTypes previous;
    private final WeatherTypes current;

    public WeatherChangeEvent(ServerLevel level, WeatherTypes.Dimension dimension,
                              WeatherTypes previous, WeatherTypes current) {
        this.level = level;
        this.dimension = dimension;
        this.previous = previous;
        this.current = current;
    }

    public ServerLevel getLevel() {
        return level;
    }

    public WeatherTypes.Dimension getDimension() {
        return dimension;
    }

    public WeatherTypes getPrevious() {
        return previous;
    }

    public WeatherTypes getCurrent() {
        return current;
    }
}
