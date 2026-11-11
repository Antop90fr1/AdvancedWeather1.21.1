package net.antopfr.advancedweather.weather.wind;

import net.antopfr.advancedweather.weather.DimensionProfile;
import net.antopfr.advancedweather.weather.WeatherManager;
import net.antopfr.advancedweather.weather.WeatherTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class WindField {

    private WindField() {}

    private static final double DIRECTION_DRIFT = 0.00004;
    private static final double VEER_SPEED = 0.0011;
    private static final double VEER_ANGLE = 0.25;

    public static final double GUST_SCALE = 72.0;

    public static final double GUST_DETAIL_SCALE = 26.0;
    public static final double GUST_DETAIL_DRIFT = 1.4;

    public static final double SWAY_SCALE = 160.0;

    private static final WindState CLIENT = new WindState();

    public static WindState clientState() {
        return CLIENT;
    }

    public static WindState state(Level level) {
        if (level instanceof ServerLevel server) {
            return WeatherManager.get(server).getWindState(server);
        }
        return CLIENT;
    }

    public static float baseIntensity(Level level) {
        return state(level).intensity();
    }

    public static float baseSpeedKmh(Level level) {
        return WindScale.intensityToKmh(baseIntensity(level));
    }

    public static Vec3 prevailingDirection(Level level, float partialTick) {
        double t = level.getGameTime() + partialTick;
        double phase = DimensionProfile.getDimension(level).ordinal() * 1913.0;

        double angle = slowNoise(t * DIRECTION_DRIFT, phase) * Math.PI * 2.0
                + slowNoise(t * VEER_SPEED, phase + 500.0) * VEER_ANGLE;
        return new Vec3(Math.cos(angle), 0, Math.sin(angle));
    }

    public static WindSample sample(Level level, BlockPos pos) {
        return sample(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 0f);
    }

    public static WindSample sample(Level level, Vec3 pos) {
        return sample(level, pos.x, pos.y, pos.z, 0f);
    }

    public static WindSample sample(Level level, double x, double y, double z) {
        return sample(level, x, y, z, 0f);
    }

    public static WindSample sample(Level level, double x, double y, double z, float partialTick) {
        return sample(level, prevailingDirection(level, partialTick), x, y, z, partialTick);
    }

    public static WindSample sample(Level level, Vec3 prevailing, double x, double y, double z, float partialTick) {
        WindState state = state(level);
        float intensity = state.intensity();
        double driftX = state.driftX(partialTick);
        double driftZ = state.driftZ(partialTick);

        double ax = x - driftX;
        double az = z - driftZ;

        float gust = gust(ax, az, x - driftX * GUST_DETAIL_DRIFT, z - driftZ * GUST_DETAIL_DRIFT);
        float amplitude = gustAmplitude(intensity);
        float multiplier = (1f - 0.5f * amplitude) + 1.5f * amplitude * gust;

        float swayNoise = GustNoise.sample(ax / SWAY_SCALE + 37.1, az / SWAY_SCALE + 59.3);
        double sway = (swayNoise - 0.5) * 0.7 * amplitude;
        Vec3 direction = prevailing.yRot((float) sway);

        float kmh = WindScale.intensityToKmh(intensity) * multiplier * altitudeFactor(level, y);
        return new WindSample(direction, kmh, gust);
    }

    public static float gust(double broadX, double broadZ, double detailX, double detailZ) {
        float broad = GustNoise.sample(broadX / GUST_SCALE, broadZ / GUST_SCALE);
        float detail = GustNoise.sample(detailX / GUST_DETAIL_SCALE + 91.7, detailZ / GUST_DETAIL_SCALE + 13.3);
        return smoothstep(0.32f, 0.78f, broad * 0.62f + detail * 0.38f);
    }

    public static float gustAmplitude(float intensity) {
        return 0.25f + 0.5f * intensity;
    }

    public static float altitudeFactor(Level level, double y) {
        if (DimensionProfile.getDimension(level) != WeatherTypes.Dimension.OVERWORLD) return 1f;
        double above = Math.max(0.0, y - level.getSeaLevel());
        return (float) (1.0 + 0.35 * Math.log1p(above / 16.0));
    }

    private static double slowNoise(double t, double offset) {
        double raw = Math.sin(t + offset)
                + Math.sin(t * 2.3 + offset * 1.7) * 0.5
                + Math.sin(t * 5.1 + offset * 0.9) * 0.25;
        return raw / 1.75;
    }

    private static float smoothstep(float edge0, float edge1, float x) {
        float t = Math.max(0f, Math.min(1f, (x - edge0) / (edge1 - edge0)));
        return t * t * (3f - 2f * t);
    }
}
