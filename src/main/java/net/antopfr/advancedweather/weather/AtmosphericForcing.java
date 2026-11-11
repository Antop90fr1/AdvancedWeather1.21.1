package net.antopfr.advancedweather.weather;

import net.minecraft.nbt.CompoundTag;

public class AtmosphericForcing {

    public enum Bias {
        NONE, SEEDING, DISSIPATING, COOLING, HEATING
    }

    private static final int RAMP_TICKS = 100;
    private static final float FADE_SHARE = 0.35f;

    private final float pressureOffset;
    private final float dewPointOffset;
    private final float tempOffset;
    private final Bias bias;
    private int remainingTicks;
    private final int totalTicks;

    public AtmosphericForcing(float pressureOffset, float dewPointOffset,
                              float tempOffset, int durationTicks, Bias bias) {
        this(pressureOffset, dewPointOffset, tempOffset, bias,
                Math.max(1, durationTicks), Math.max(1, durationTicks));
    }

    private AtmosphericForcing(float pressureOffset, float dewPointOffset, float tempOffset,
                               Bias bias, int remaining, int total) {
        this.pressureOffset = pressureOffset;
        this.dewPointOffset = dewPointOffset;
        this.tempOffset = tempOffset;
        this.bias = bias != null ? bias : Bias.NONE;
        this.remainingTicks = remaining;
        this.totalTicks = Math.max(1, total);
    }

    public boolean tick() {
        remainingTicks--;
        return remainingTicks > 0;
    }

    public float pressureOffset() { return pressureOffset; }
    public float dewPointOffset() { return dewPointOffset; }
    public float tempOffset() { return tempOffset; }
    public Bias bias() { return bias; }
    public int remainingTicks() { return remainingTicks; }

    public float strength() { return strengthIn(0); }

    public float strengthIn(int ticksAhead) {
        int remaining = remainingTicks - ticksAhead;
        if (remaining <= 0) return 0f;

        int ramp = Math.min(RAMP_TICKS, Math.max(1, totalTicks / 4));
        float in = Math.min(1f, (totalTicks - remaining + 1) / (float) ramp);
        float out = Math.min(1f, remaining / Math.max(1f, totalTicks * FADE_SHARE));
        return Math.min(in, out);
    }

    public AtmosphericForcing advanced(int ticks) {
        int remaining = remainingTicks - ticks;
        if (remaining <= 0) return null;
        return new AtmosphericForcing(pressureOffset, dewPointOffset, tempOffset, bias, remaining, totalTicks);
    }

    public static float biasModifier(WeatherTypes target, AtmosphericForcing.Bias bias) {
        return switch (bias) {
            case SEEDING -> switch (target) {
                case LIGHT_RAIN, HEAVY_RAIN, DRIZZLE, THUNDERSTORM, OVERCAST, SNOW -> 2.2f;
                case CLEAR, SUNNY -> 0.4f;
                default -> 1f;
            };
            case DISSIPATING -> switch (target) {
                case CLEAR, SUNNY -> 2.2f;
                case LIGHT_RAIN, HEAVY_RAIN, THUNDERSTORM, OVERCAST, DRIZZLE,
                     SNOW, BLIZZARD, FREEZING_RAIN, HAIL -> 0.4f;
                default -> 1f;
            };
            case COOLING -> switch (target) {
                case SNOW, FREEZING_RAIN, BLIZZARD -> 3.0f;
                case HAIL -> 1.5f;
                case THUNDERSTORM, SANDSTORM -> 0.5f;
                default -> 1f;
            };
            case HEATING -> switch (target) {
                case SNOW, BLIZZARD, FREEZING_RAIN -> 0.2f;
                case THUNDERSTORM -> 1.8f;
                case FOG, DENSE_FOG, MIST -> 0.5f;
                default -> 1f;
            };
            case NONE -> 1f;
        };
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putFloat("pOffset", pressureOffset);
        tag.putFloat("dpOffset", dewPointOffset);
        tag.putFloat("tempOff", tempOffset);
        tag.putString("bias", bias.name());
        tag.putInt("remaining", remainingTicks);
        tag.putInt("total", totalTicks);
        return tag;
    }

    public static AtmosphericForcing load(CompoundTag tag) {
        Bias bias = Bias.NONE;
        if (tag.contains("bias")) {
            try { bias = Bias.valueOf(tag.getString("bias")); }
            catch (IllegalArgumentException ignored) {}
        }
        return new AtmosphericForcing(
                tag.getFloat("pOffset"), tag.getFloat("dpOffset"), tag.getFloat("tempOff"),
                bias, tag.getInt("remaining"), tag.getInt("total"));
    }
}
