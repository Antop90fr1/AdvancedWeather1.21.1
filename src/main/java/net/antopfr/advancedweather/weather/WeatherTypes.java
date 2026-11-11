package net.antopfr.advancedweather.weather;

import net.minecraft.network.chat.Component;
import net.minecraft.world.level.biome.Biome;

public enum WeatherTypes {

    // OVERWORLD
    CLEAR("advancedweather.types.clear", false, false, Dimension.OVERWORLD),
    SUNNY("advancedweather.types.sunny", false, false, Dimension.OVERWORLD),
    CLOUDY("advancedweather.types.cloudy", false, false, Dimension.OVERWORLD),
    OVERCAST("advancedweather.types.overcast", false, false, Dimension.OVERWORLD),
    MIST("advancedweather.types.mist", false, false, Dimension.OVERWORLD),
    DRIZZLE("advancedweather.types.drizzle", true, false, Dimension.OVERWORLD),
    LIGHT_RAIN("advancedweather.types.light_rain", true, false, Dimension.OVERWORLD),
    HEAVY_RAIN("advancedweather.types.heavy_rain", true, false, Dimension.OVERWORLD),
    FREEZING_RAIN("advancedweather.types.freezing_rain", true, false, Dimension.OVERWORLD),
    THUNDERSTORM("advancedweather.types.thunderstorm", true, true, Dimension.OVERWORLD),
    SNOW("advancedweather.types.snow", true, false, Dimension.OVERWORLD),
    BLIZZARD("advancedweather.types.blizzard", true, false, Dimension.OVERWORLD),
    HAIL("advancedweather.types.hail", true, false, Dimension.OVERWORLD),
    FOG("advancedweather.types.fog", false, false, Dimension.OVERWORLD),
    DENSE_FOG("advancedweather.types.dense_fog", false, false, Dimension.OVERWORLD),
    WINDY("advancedweather.types.windy", false, false, Dimension.OVERWORLD),
    SANDSTORM("advancedweather.types.sandstorm", false, false, Dimension.OVERWORLD),

    // NETHER
    NETHER_CLEAR("advancedweather.types.nether_clear", false, false, Dimension.NETHER),
    BRIMSTONE_STORM("advancedweather.types.brimstone_storm", false, false, Dimension.NETHER),
    LAVA_RAIN("advancedweather.types.lava_rain", false, false, Dimension.NETHER),
    ASH_STORM("advancedweather.types.ash_storm", false, false, Dimension.NETHER),
    NETHERSTORM("advancedweather.types.netherstorm", false, true, Dimension.NETHER),
    HELLFIRE("advancedweather.types.hellfire", false, false, Dimension.NETHER),

    // END
    END_CLEAR("advancedweather.types.end_clear", false, false, Dimension.END),
    VOID_STORM("advancedweather.types.void_storm", false, false, Dimension.END),
    END_MIST("advancedweather.types.end_mist", false, false, Dimension.END),
    CHORUS_GALE("advancedweather.types.chorus_gale", false, false, Dimension.END),
    ENDERSTORM("advancedweather.types.enderstorm", false, true, Dimension.END);

    public enum Dimension { OVERWORLD, NETHER, END }

    private final String    translationKey;
    private final boolean   vanillaRaining;
    private final boolean   vanillaThundering;
    public  final Dimension dimension;

    WeatherTypes(String translationKey, boolean rain, boolean thunder, Dimension dim) {
        this.translationKey = translationKey;
        this.vanillaRaining = rain;
        this.vanillaThundering = thunder;
        this.dimension = dim;
    }

    public String translationKey() { return translationKey; }

    public Component displayName() { return Component.translatable(translationKey); }
    public String displayString() { return Component.translatable(translationKey).getString(); }

    public boolean isVanillaRaining()   { return vanillaRaining; }
    public boolean isVanillaThundering(){ return vanillaThundering; }

    public Biome.Precipitation precipitation() {
        return switch (this) {
            case SNOW, BLIZZARD -> Biome.Precipitation.SNOW;
            case DRIZZLE, LIGHT_RAIN, HEAVY_RAIN, FREEZING_RAIN, THUNDERSTORM, HAIL -> Biome.Precipitation.RAIN;
            default -> Biome.Precipitation.NONE;
        };
    }

    public Dimension dimension()        { return dimension; }

    public boolean isOverworld() { return dimension == Dimension.OVERWORLD; }
    public boolean isNether()    { return dimension == Dimension.NETHER; }
    public boolean isEnd()       { return dimension == Dimension.END; }

    public boolean hasFog() {
        return this == FOG || this == DENSE_FOG || this == BLIZZARD
                || this == FREEZING_RAIN || this == THUNDERSTORM
                || this == SANDSTORM || this == HAIL
                || this == BRIMSTONE_STORM || this == ASH_STORM || this == NETHERSTORM || this == HELLFIRE
                || this == END_MIST || this == ENDERSTORM || this == VOID_STORM || this == CHORUS_GALE;
    }

    public static WeatherTypes fromNameSafe(String name) {
        if (name == null) return CLEAR;
        try { return valueOf(name.trim().toUpperCase()); }
        catch (IllegalArgumentException e) { return CLEAR; }
    }
}