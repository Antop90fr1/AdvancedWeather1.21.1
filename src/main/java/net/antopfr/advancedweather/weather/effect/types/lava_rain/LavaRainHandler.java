package net.antopfr.advancedweather.weather.effect.types.lava_rain;

import net.antopfr.advancedweather.client.particle.AWParticles;
import net.antopfr.advancedweather.client.state.ClientWeatherState;
import net.antopfr.advancedweather.client.wind.ClientWind;
import net.antopfr.advancedweather.compat.particlerain.ParticleRainDrops;
import net.antopfr.advancedweather.weather.WeatherTypes;
import net.antopfr.advancedweather.weather.wind.WindField;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = "advancedweather", value = Dist.CLIENT)
public final class LavaRainHandler {

    private LavaRainHandler() {}

    private static final int RADIUS = 18;
    private static final int ATTEMPTS = 36;
    private static final double MAX_SPAWN_HEIGHT = 26.0;
    private static final float MINIMAL_SHARE = 0.35f;
    private static final float STORM_DROPS = 1.6f;
    private static final int RAIN_TINT = 0xFF7A1F;

    private static final double HEARING_RANGE = 14.0;
    private static final int SOUNDS_PER_TICK = 2;
    private static final int RUMBLE_INTERVAL = 50;

    private static int soundsThisTick;
    private static int rumbleDelay;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        soundsThisTick = 0;

        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null || mc.isPaused()) return;

        float strength = ClientWeatherState.presence(WeatherTypes.LAVA_RAIN, 1f)
                + STORM_DROPS * ClientWeatherState.presence(WeatherTypes.NETHERSTORM, 1f);
        if (strength <= 0f) return;

        RandomSource random = level.random;
        Vec3 player = mc.player.position();

        boolean minimal = mc.options.particles().get() == ParticleStatus.MINIMAL;
        float share = strength * (minimal ? MINIMAL_SHARE : 1f);

        ParticleRainDrops.Shower shower = ParticleRainDrops.shower();
        int drops = shower != null
                ? fallAsParticleRain(mc, level, shower, share)
                : fallAsStreaks(level, player, share, minimal);

        if (drops > 0 && --rumbleDelay <= 0) {
            rumbleDelay = RUMBLE_INTERVAL;
            level.playLocalSound(player.x, player.y, player.z, SoundEvents.LAVA_AMBIENT, SoundSource.WEATHER,
                    0.3f * strength, 0.8f + random.nextFloat() * 0.3f, false);
        }
    }

    private static int fallAsParticleRain(Minecraft mc, ClientLevel level, ParticleRainDrops.Shower shower, float share) {
        Vec3 camera = mc.gameRenderer.getMainCamera().getPosition();

        int drops = 0;
        int attempts = Math.round(shower.drops() * share);
        for (int i = 0; i < attempts; i++) {
            Vec3 point = shower.point(level.random, camera);

            LavaRainSky.Opening opening = LavaRainSky.below(level, Mth.floor(point.x), point.y, Mth.floor(point.z));
            if (opening == null) continue;

            double y = Math.min(opening.roof() - 0.3, point.y);
            if (ParticleRainDrops.fall(level, point.x, y, point.z, RAIN_TINT, LavaDropParticle::land)) drops++;
        }
        return drops;
    }

    private static int fallAsStreaks(ClientLevel level, Vec3 player, float share, boolean minimal) {
        RandomSource random = level.random;
        Vec3 prevailing = ClientWind.direction();
        double top = player.y + MAX_SPAWN_HEIGHT;

        int drops = 0;
        int attempts = Math.round(ATTEMPTS * share);
        for (int i = 0; i < attempts; i++) {
            double x = player.x + (random.nextDouble() - 0.5) * 2 * RADIUS;
            double z = player.z + (random.nextDouble() - 0.5) * 2 * RADIUS;

            LavaRainSky.Opening opening = LavaRainSky.below(level, Mth.floor(x), top, Mth.floor(z));
            if (opening == null) continue;

            double y = Math.min(opening.roof() - 0.3, top);
            Vec3 wind = WindField.sample(level, prevailing, x, y, z, 0f).velocity();
            level.addParticle(AWParticles.LAVA_DROP.get(), minimal, x, y, z, wind.x, 0.0, wind.z);
            drops++;
        }
        return drops;
    }

    static void impact(ClientLevel level, double x, double y, double z) {
        if (soundsThisTick >= SOUNDS_PER_TICK) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.distanceToSqr(x, y, z) > HEARING_RANGE * HEARING_RANGE) return;

        RandomSource random = level.random;
        if (random.nextFloat() > 0.3f) return;

        soundsThisTick++;
        if (random.nextInt(5) == 0) {
            level.playLocalSound(x, y, z, SoundEvents.LAVA_EXTINGUISH, SoundSource.WEATHER,
                    0.12f, 1.5f + random.nextFloat() * 0.7f, false);
        } else {
            level.playLocalSound(x, y, z, SoundEvents.LAVA_POP, SoundSource.WEATHER,
                    0.35f, 0.7f + random.nextFloat() * 0.8f, false);
        }
    }
}
