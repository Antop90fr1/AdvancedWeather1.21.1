package net.antopfr.advancedweather.weather;

import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class WeatherDefinition {

    public final float tempBase;
    public final float windBase;
    public final float recommendedPressure;
    public final float recommendedDewPoint;
    public final int minDuration, maxDuration;
    public final float cloudAltitude;
    public final float cloudCoverage;
    public final float coverageMargin;
    public final Vec3 cloudTint;
    public final float cloudOpacity;
    public final CloudShape cloudShape;
    public final float skyDesaturation;
    public final float rainIntensity;
    public final boolean hidesSun;
    public final float fogNear, fogFar;

    public final List<WeatherEffects> effects;

    private WeatherDefinition(Builder b) {
        this.tempBase = b.tempBase;
        this.windBase = b.windBase;
        this.recommendedPressure = b.pressure;
        this.recommendedDewPoint = b.dewPoint;
        this.minDuration = b.minDuration;
        this.maxDuration = b.maxDuration;
        this.cloudAltitude = b.cloudAltitude;
        this.cloudCoverage = b.cloudCoverage;
        this.coverageMargin = b.coverageMargin;
        this.cloudTint = b.cloudTint;
        this.cloudOpacity = b.cloudOpacity;
        this.cloudShape = b.cloudShape;
        this.skyDesaturation = b.skyDesaturation;
        this.rainIntensity = b.rainIntensity;
        this.hidesSun = b.hidesSun;
        this.fogNear = b.fogNear;
        this.fogFar = b.fogFar;
        this.effects = List.copyOf(b.effects);
    }

    public static Builder of(WeatherTypes type) {
        return new Builder(type);
    }

    public static final class Builder {
        private final WeatherTypes type;
        private float tempBase = 15f, windBase = 0.1f;
        private float pressure = 1013f, dewPoint = 8f;
        private int minDuration = 6000, maxDuration = 12000;
        private float cloudAltitude = 160f;
        private float cloudCoverage = 0f;
        private float coverageMargin = 0.08f;
        private Vec3 cloudTint = new Vec3(1, 1, 1);
        private float cloudOpacity = 0.5f, skyDesaturation = 0f;
        private CloudShape cloudShape = CloudShape.CUMULUS;
        private float rainIntensity = -1f;
        private boolean hidesSun = false;
        private float fogNear = -1f, fogFar = -1f;
        private final List<WeatherEffects> effects = new ArrayList<>();

        private Builder(WeatherTypes type) { this.type = type; }

        public Builder temperature(float c)       { this.tempBase = c; return this; }
        public Builder wind(float i)              { this.windBase = i; return this; }
        public Builder pressure(float hPa)        { this.pressure = hPa; return this; }
        public Builder dewPoint(float c)          { this.dewPoint = c; return this; }
        public Builder duration(int min, int max) { this.minDuration = min; this.maxDuration = max; return this; }
        public Builder clouds(Vec3 tint, float opacity) { this.cloudTint = tint; this.cloudOpacity = opacity; return this; }
        public Builder cloudAltitude(float y)     { this.cloudAltitude = y; return this; }
        public Builder coverage(float base, float margin) { this.cloudCoverage = base; this.coverageMargin = margin; return this; }
        public Builder cloudShape(CloudShape shape) { this.cloudShape = shape; return this; }
        public Builder skyDesaturation(float d)   { this.skyDesaturation = d; return this; }
        public Builder rainIntensity(float r)     { this.rainIntensity = r; return this; }
        public Builder hidesSun(boolean s)        { this.hidesSun = s; return this; }
        public Builder fog(float near, float far) { this.fogNear = near; this.fogFar = far; return this; }
        public Builder effects(WeatherEffects... e) { this.effects.addAll(Arrays.asList(e)); return this; }

        public WeatherDefinition build() {
            WeatherDefinition def = new WeatherDefinition(this);
            WeatherDefinitions.register(type, def);
            return def;
        }
    }
}
