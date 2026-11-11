package net.antopfr.advancedweather.weather.wind;

import net.minecraft.network.chat.Component;

public final class WindScale {

    private WindScale() {}

    public static final float MAX_KMH = 120f;

    public static float intensityToKmh(float intensity) {
        return intensity * intensity * MAX_KMH;
    }

    public static float kmhToIntensity(float kmh) {
        return (float) Math.sqrt(Math.max(0f, kmh) / MAX_KMH);
    }

    public static double kmhToBlocksPerTick(double kmh) {
        return kmh / 72.0;
    }

    public static String beaufortKey(float kmh) {
        String name;
        if (kmh < 1)        name = "calm";
        else if (kmh < 6)   name = "light_air";
        else if (kmh < 12)  name = "light_breeze";
        else if (kmh < 20)  name = "gentle_breeze";
        else if (kmh < 29)  name = "moderate_breeze";
        else if (kmh < 39)  name = "fresh_breeze";
        else if (kmh < 50)  name = "strong_breeze";
        else if (kmh < 62)  name = "high_wind_near_gale";
        else if (kmh < 75)  name = "gale";
        else if (kmh < 89)  name = "strong_gale";
        else if (kmh < 103) name = "storm";
        else if (kmh < 118) name = "violent_storm";
        else                name = "hurricane";
        return "advancedweather.wind." + name;
    }

    public static Component beaufort(float kmh) {
        return Component.translatable(beaufortKey(kmh));
    }
}
