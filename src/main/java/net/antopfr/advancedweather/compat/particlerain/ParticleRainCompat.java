package net.antopfr.advancedweather.compat.particlerain;

import com.mojang.blaze3d.systems.RenderSystem;
import net.antopfr.advancedweather.client.particle.AWParticles;
import net.antopfr.advancedweather.client.state.ClientWeatherState;
import net.antopfr.advancedweather.client.wind.ClientWind;
import net.antopfr.advancedweather.config.AWClientConfig;
import net.antopfr.advancedweather.util.AWLogger;
import net.antopfr.advancedweather.util.ModLoaded;
import net.antopfr.advancedweather.weather.WeatherDefinitions;
import net.antopfr.advancedweather.weather.WeatherTypes;
import net.antopfr.advancedweather.weather.wind.WindField;
import net.antopfr.advancedweather.weather.wind.WindSample;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.joml.Vector3f;

import java.lang.reflect.Method;

@EventBusSubscriber(modid = "advancedweather", value = Dist.CLIENT)
public final class ParticleRainCompat {

    private ParticleRainCompat() {}

    private static final float MAX_DENSITY = 1.5f;

    private static final float GALE_PUSH = 0.06f;
    private static final float GALE_KMH = 90f;

    private static final int HAIL_STONES = 14;
    private static final double HAIL_RADIUS = 14.0;
    private static final double HAIL_HEIGHT = 10.0;
    private static final double HAIL_HEIGHT_SPREAD = 6.0;
    private static final double HAIL_SPEED = 1.1;
    private static final double HAIL_DRIFT = 0.25;

    private static final float HAIL_MINIMAL_SHARE = 0.35f;

    private static int depth;
    private static int reported;

    private static boolean raining;
    private static Method weatherChange;
    private static boolean weatherChangeFailed;

    public static boolean enabled() {
        return ModLoaded.PARTICLERAIN && AWClientConfig.get().particleRainIntegration;
    }

    public static void enter() {
        if (!AWClientConfig.get().particleRainIntegration) return;
        depth++;
        report(1, "weather");
    }

    public static void exit() {
        if (depth > 0) depth--;
    }

    public static boolean asking() {
        return depth > 0 && RenderSystem.isOnRenderThread();
    }

    private static void report(int hook, String name) {
        if ((reported & hook) != 0) return;
        reported |= hook;
        AWLogger.AW.info("[AdvancedWeather] Particle Rain hook active: {}", name);
    }

    private static float falling(WeatherTypes type) {
        if (type == WeatherTypes.HAIL) return 0f;
        if (type == WeatherTypes.SANDSTORM) return 1f;
        return WeatherDefinitions.get(type).rainIntensity;
    }

    private static float storm(WeatherTypes type) {
        return switch (type) {
            case THUNDERSTORM, BLIZZARD, SANDSTORM -> 1f;
            case HEAVY_RAIN -> 0.6f;
            default -> 0f;
        };
    }

    private static WeatherTypes source() {
        WeatherTypes current = ClientWeatherState.getCurrentWeather();
        if (falling(current) > 0f) return current;

        WeatherTypes previous = ClientWeatherState.getPreviousWeather();
        if (falling(previous) > 0f && ClientWeatherState.getTransitionProgress() < 1f) return previous;
        return null;
    }

    private static Biome.Precipitation precipitationOf(WeatherTypes source) {
        return switch (source) {
            case SNOW, BLIZZARD -> Biome.Precipitation.SNOW;
            case SANDSTORM -> Biome.Precipitation.NONE;
            default -> Biome.Precipitation.RAIN;
        };
    }

    public static float rainLevel(float delta) {
        float from = falling(ClientWeatherState.getPreviousWeather());
        float to = falling(ClientWeatherState.getCurrentWeather());
        if (from < 0f && to < 0f) return Float.NaN;

        float t = ClientWeatherState.getSmoothedTransitionProgress(delta);
        return Mth.lerp(t, Math.max(from, 0f), Math.max(to, 0f));
    }

    public static float thunderLevel(float delta) {
        float t = ClientWeatherState.getSmoothedTransitionProgress(delta);
        return Mth.lerp(t, storm(ClientWeatherState.getPreviousWeather()), storm(ClientWeatherState.getCurrentWeather()));
    }

    public static Biome.Precipitation precipitation() {
        WeatherTypes source = source();
        return source == null ? null : precipitationOf(source);
    }

    public static int density(int usual) {
        if (!enabled()) return usual;
        report(2, "density");

        float rain = rainLevel(1f);
        if (!(rain > 0f)) return usual;
        return Math.round(usual * Math.min(Math.min(rain * 2f, 0.25f + 0.75f * rain), MAX_DENSITY));
    }

    public static Vector3f wind(double x, double y, double z) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return new Vector3f();
        report(4, "wind");

        WindSample air = WindField.sample(level, ClientWind.direction(), x, y, z, 0f);
        float push = GALE_PUSH * Mth.clamp(air.speedKmh() / GALE_KMH, 0f, 1.2f);
        Vec3 direction = air.direction();
        return new Vector3f((float) (direction.x * push), 0f, (float) (direction.z * push));
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        depth = 0;
        if (!enabled()) return;

        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            raining = false;
            return;
        }
        if (mc.isPaused()) return;

        followRain();

        float hail = ClientWeatherState.presence(WeatherTypes.HAIL, 1f);
        if (hail > 0f) hailFall(mc, level, hail);
    }

    private static void followRain() {
        WeatherTypes source = source();
        boolean now = source != null && precipitationOf(source) == Biome.Precipitation.RAIN;
        if (now == raining) return;
        raining = now;

        if (weatherChangeFailed) return;
        try {
            if (weatherChange == null) {
                weatherChange = Class.forName("pigcart.particlerain.ParticleSpawner")
                        .getMethod("onWeatherChange", boolean.class);
            }
            weatherChange.invoke(null, now);
        } catch (ReflectiveOperationException | RuntimeException e) {
            weatherChangeFailed = true;
            AWLogger.AW.warn("[AdvancedWeather] Particle Rain will not be told when the rain stops: {}", e.toString());
        }
    }

    private static void hailFall(Minecraft mc, ClientLevel level, float strength) {
        RandomSource random = level.random;
        Vec3 camera = mc.gameRenderer.getMainCamera().getPosition();
        Vec3 drift = ClientWind.direction().scale(HAIL_DRIFT * ClientWind.intensity());

        boolean minimal = mc.options.particles().get() == ParticleStatus.MINIMAL;

        int stones = Math.round(HAIL_STONES * strength * (minimal ? HAIL_MINIMAL_SHARE : 1f));
        for (int i = 0; i < stones; i++) {
            double x = camera.x + (random.nextDouble() - 0.5) * 2 * HAIL_RADIUS;
            double z = camera.z + (random.nextDouble() - 0.5) * 2 * HAIL_RADIUS;
            double y = camera.y + HAIL_HEIGHT + random.nextDouble() * HAIL_HEIGHT_SPREAD;
            if (y <= level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(x), Mth.floor(z))) continue;

            level.addParticle(AWParticles.HAIL_DROP.get(), minimal, x, y, z,
                    drift.x, -(HAIL_SPEED + random.nextDouble() * 0.3), drift.z);
        }
    }
}
