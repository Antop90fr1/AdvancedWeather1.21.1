package net.antopfr.advancedweather.weather.effect.types.void_storm;

public final class VoidPulse {

    private VoidPulse() {}

    private static final double RATE = 0.08;

    public static double phase(long gameTime, float partialTick) {
        return (gameTime + (double) partialTick) * RATE;
    }

    public static float surge(long gameTime, float partialTick) {
        return 0.5f - 0.5f * (float) Math.sin(phase(gameTime, partialTick));
    }
}
