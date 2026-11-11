package net.antopfr.advancedweather.weather;

import net.minecraft.util.Mth;

public class ForecastConfidence {

    public static float compute(float[] weights, AtmosphericSystem atmosphere) {
        if (weights.length == 0) return 98f;

        float total = 0f, best = 0f;
        for (float w : weights) {
            total += w;
            best = Math.max(best, w);
        }
        float share = total > 0f ? best / total : 0f;
        return Mth.clamp(share * stability(atmosphere) * 100f, 5f, 98f);
    }

    private static float stability(AtmosphericSystem atmosphere) {
        float instability = Mth.clamp(Math.abs(atmosphere.getPressureVel()) / 1.5f, 0f, 1f);
        return Mth.lerp(instability, 1.0f, 0.55f);
    }
}
