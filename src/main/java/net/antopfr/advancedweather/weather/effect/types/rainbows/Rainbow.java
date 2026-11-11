package net.antopfr.advancedweather.weather.effect.types.rainbows;

import net.minecraft.util.Mth;

public final class Rainbow {

    private Rainbow() {}

    public static final int FADE_IN_TICKS = 100;
    public static final int FADE_OUT_TICKS = 200;

    public static float sunElevationDeg(float timeOfDay) {
        return (float) Math.toDegrees(Math.asin(Mth.cos(timeOfDay * Mth.TWO_PI)));
    }

    public static float sunFactor(float sunElevationDeg) {
        float risen = Mth.clamp((sunElevationDeg - 1f) / 5f, 0f, 1f);
        float lowEnough = Mth.clamp((42f - sunElevationDeg) / 8f, 0f, 1f);
        return Math.min(risen, lowEnough);
    }

    public static float envelope(double gameTime, long start, long end) {
        if (gameTime <= start || gameTime >= end) return 0f;
        float in = (float) (gameTime - start) / FADE_IN_TICKS;
        float out = (float) (end - gameTime) / FADE_OUT_TICKS;
        return Mth.clamp(Math.min(in, out), 0f, 1f);
    }
}
