package net.antopfr.advancedweather.compat.particlerain;

import net.antopfr.advancedweather.util.AWLogger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.Vec3;

import java.awt.Color;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ParticleRainDrops {

    private ParticleRainDrops() {}

    private static final double CALM_SPEED = 0.8;
    private static final float WIND_SHARE = 1f;
    private static final int AMOUNT_PER_TICK = 25;

    private static boolean prepared;
    private static boolean failed;
    private static boolean reported;

    private static Method getConfig;
    private static Field perf;
    private static Field compat;
    private static Field waterTint;
    private static Field density;
    private static Field distance;
    private static Field maxDrops;
    private static Field amount;
    private static Field liveDrops;
    private static Field particles;
    private static Constructor<?> newData;
    private static Constructor<?> newDrop;
    private static Object customTint;
    private static Object worldVelocity;

    private static Object rain;
    private static final Map<Integer, Object> TINTED = new HashMap<>();

    public record Shower(int drops, float radius, boolean calm) {

        public Vec3 point(RandomSource random, Vec3 camera) {
            float first = random.nextFloat();
            float second = random.nextFloat();
            float height = calm ? 1f - Mth.abs(Mth.square(first) - Mth.square(second)) : first;
            float around = Mth.TWO_PI * random.nextFloat();
            float tilt = (float) Math.acos(2f * height - 1f);

            return new Vec3(radius * Mth.sin(tilt) * Mth.cos(around) + camera.x,
                    radius * Mth.cos(tilt) + camera.y,
                    radius * Mth.sin(tilt) * Mth.sin(around) + camera.z);
        }
    }

    public static Shower shower() {
        if (failed || !ParticleRainCompat.enabled()) return null;
        try {
            if (!prepared) prepare();

            Object config = getConfig.invoke(null);
            if (!waterTint.getBoolean(compat.get(config))) return null;

            Object performance = perf.get(config);
            Entity viewer = Minecraft.getInstance().getCameraEntity();
            double speed = viewer == null ? 0.0 : viewer.getDeltaMovement().length();

            return new Shower(drops(performance, speed), distance.getInt(performance), speed < CALM_SPEED);
        } catch (ReflectiveOperationException | RuntimeException e) {
            giveUp(e);
            return null;
        }
    }

    private static int drops(Object performance, double speed) throws IllegalAccessException {
        int live = liveDrops.getInt(null);
        if (amount != null) {
            int most = amount.getInt(performance);
            return live >= most ? 0 : most / AMOUNT_PER_TICK * (1 + 2 * (int) speed);
        }
        return live >= maxDrops.getInt(performance) ? 0 : (int) (density.getInt(performance) * (1.0 + speed));
    }

    public static boolean fall(ClientLevel level, double x, double y, double z, int tint,
                               ParticleRainDrop.Landing landing) {
        if (failed) return false;
        try {
            Particle drop = (Particle) newDrop.newInstance(level, x, y, z, tinted(tint));
            ((ParticleRainDrop) drop).advancedweather$onLanding(landing);
            Minecraft.getInstance().particleEngine.add(drop);

            if (!reported) {
                reported = true;
                AWLogger.AW.info("[AdvancedWeather] Particle Rain hook active: drops");
            }
            return true;
        } catch (ReflectiveOperationException | RuntimeException e) {
            giveUp(e);
            return false;
        }
    }

    private static void prepare() throws ReflectiveOperationException {
        Class<?> manager = Class.forName("pigcart.particlerain.config.ConfigManager");
        Class<?> config = Class.forName("pigcart.particlerain.config.ConfigData");
        Class<?> performance = Class.forName("pigcart.particlerain.config.ConfigData$PerformanceOptions");
        Class<?> compatibility = Class.forName("pigcart.particlerain.config.ConfigData$CompatibilityOptions");
        Class<?> data = Class.forName("pigcart.particlerain.config.ParticleData");

        getConfig = manager.getMethod("getConfig");
        perf = config.getField("perf");
        compat = config.getField("compat");
        waterTint = compatibility.getField("waterTint");
        distance = performance.getField("particleDistance");
        try {
            amount = performance.getField("particleAmount");
        } catch (NoSuchFieldException older) {
            density = performance.getField("particleDensity");
            maxDrops = performance.getField("maxParticleAmount");
        }
        liveDrops = Class.forName("pigcart.particlerain.ParticleSpawner").getField("particleCount");
        particles = Class.forName("pigcart.particlerain.ParticleLoader").getField("particles");
        newData = data.getConstructor();
        newDrop = Class.forName("pigcart.particlerain.particle.CustomParticle")
                .getConstructor(ClientLevel.class, double.class, double.class, double.class, data);
        customTint = Class.forName("pigcart.particlerain.config.ParticleData$TintType").getField("CUSTOM").get(null);
        worldVelocity = Class.forName("pigcart.particlerain.config.ParticleData$RotationType")
                .getField("WORLD_VELOCITY").get(null);
        prepared = true;
    }

    private static Object tinted(int tint) throws ReflectiveOperationException {
        Object source = ((Map<?, ?>) particles.get(null)).get("rain");
        if (source != rain) {
            rain = source;
            TINTED.clear();
        }

        Object data = TINTED.get(tint);
        if (data == null) {
            data = copy(source, tint);
            TINTED.put(tint, data);
        }
        return data;
    }

    private static Object copy(Object source, int tint) throws ReflectiveOperationException {
        Object data = newData.newInstance();
        for (Field field : source.getClass().getFields()) {
            int modifiers = field.getModifiers();
            if (!Modifier.isStatic(modifiers) && !Modifier.isFinal(modifiers)) field.set(data, field.get(source));
        }

        Class<?> type = data.getClass();
        type.getField("tintType").set(data, customTint);
        type.getField("customTint").set(data, new Color(tint));
        type.getField("rotationType").set(data, worldVelocity);
        type.getField("windStrength").set(data, WIND_SHARE);
        type.getField("stormWindStrength").set(data, WIND_SHARE);
        type.getField("precipitation").set(data, new ArrayList<>(List.of(Biome.Precipitation.values())));
        return data;
    }

    private static void giveUp(Exception e) {
        failed = true;
        AWLogger.AW.warn("[AdvancedWeather] Particle Rain drops will not be used: {}", e.toString());
    }
}
