package net.antopfr.advancedweather.weather;

public enum WeatherEffects {
    // OVERWORLD
    WIND_LINES        ("Wind Lines",         WeatherTypes.Dimension.OVERWORLD),
    GROUND_FOG        ("Ground Fog",         WeatherTypes.Dimension.OVERWORLD),
    BLIZZARD_PARTICLES("Blizzard Particles", WeatherTypes.Dimension.OVERWORLD),
    FOG_PARTICLES     ("Fog Particles",      WeatherTypes.Dimension.OVERWORLD),
    SAND_PARTICLES    ("Sand Particles",     WeatherTypes.Dimension.OVERWORLD),
    TUMBLEWEEDS       ("Tumbleweeds",        WeatherTypes.Dimension.OVERWORLD),
    RAINBOWS          ("Rainbows",           WeatherTypes.Dimension.OVERWORLD),

    // NETHER
    NETHER_WIND_LINES  ("Nether Wind Lines",   WeatherTypes.Dimension.NETHER),
    BRIMSTONE_PARTICLES("Brimstone Particles", WeatherTypes.Dimension.NETHER),
    ASH_PARTICLES      ("Ash Particles",       WeatherTypes.Dimension.NETHER),
    FIRE_PARTICLES     ("Fire Particles",      WeatherTypes.Dimension.NETHER),
    HEAT_SHIMMER       ("Heat Shimmer",        WeatherTypes.Dimension.NETHER),

    // END
    END_WIND_LINES     ("End Wind Lines",      WeatherTypes.Dimension.END),
    END_GROUND_FOG     ("End Ground Fog",      WeatherTypes.Dimension.END),
    VOID_PARTICLES     ("Void Particles",      WeatherTypes.Dimension.END),
    ENDER_PARTICLES    ("Ender Particles",     WeatherTypes.Dimension.END),
    CHROMATIC_EFFECT   ("Chromatic Effect",    WeatherTypes.Dimension.END),
    CHORUS_PLANTS      ("Chorus Plants",       WeatherTypes.Dimension.END),
    CHORUS_PETALS      ("Chorus Petals",       WeatherTypes.Dimension.END);

    private final String name;
    public final WeatherTypes.Dimension allowedDimension;

    WeatherEffects(String name, WeatherTypes.Dimension dim) {
        this.name = name;
        this.allowedDimension = dim;
    }

    public String effectName() { return name; }

    public boolean isAllowedIn(WeatherTypes.Dimension dim) {
        return allowedDimension == null || allowedDimension == dim;
    }

    public static WeatherEffects fromNameSafe(String name) {
        if (name == null) return null;
        try { return valueOf(name.trim().toUpperCase()); }
        catch (IllegalArgumentException e) { return null; }
    }
}