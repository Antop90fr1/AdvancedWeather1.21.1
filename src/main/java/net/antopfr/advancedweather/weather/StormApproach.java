package net.antopfr.advancedweather.weather;

public final class StormApproach {

    private StormApproach() {}

    public static final int LEAD_TICKS = 20 * 90;

    private static final int GUST_TICKS = 20 * 15;
    private static final float GUST_SHARE = 1.1f;

    public static int lead(WeatherTypes weather) {
        return switch (weather) {
            case THUNDERSTORM, SANDSTORM, NETHERSTORM, ENDERSTORM -> LEAD_TICKS;
            case HAIL -> 20 * 60;
            default -> 0;
        };
    }

    public static boolean announces(WeatherTypes weather) {
        return lead(weather) > 0;
    }

    public static float gust(WeatherTypes coming, int ticksAway) {
        if (ticksAway < 0 || ticksAway > GUST_TICKS) return 0f;
        float front = Math.min(1f, WeatherDefinitions.get(coming).windBase * GUST_SHARE);
        return front * (1f - ticksAway / (float) GUST_TICKS);
    }
}
