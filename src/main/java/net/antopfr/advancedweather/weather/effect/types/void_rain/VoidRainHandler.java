package net.antopfr.advancedweather.weather.effect.types.void_rain;

import net.antopfr.advancedweather.client.particle.AWParticles;
import net.antopfr.advancedweather.client.state.ClientWeatherState;
import net.antopfr.advancedweather.client.wind.ClientWind;
import net.antopfr.advancedweather.compat.particlerain.ParticleRainDrops;
import net.antopfr.advancedweather.weather.WeatherEffects;
import net.antopfr.advancedweather.weather.WeatherTypes;
import net.antopfr.advancedweather.weather.wind.WindField;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = "advancedweather", value = Dist.CLIENT)
public final class VoidRainHandler {

    private VoidRainHandler() {}

    private static final int RADIUS = 20;
    private static final int DROPS = 30;
    private static final double HEIGHT = 14.0;
    private static final double HEIGHT_SPREAD = 10.0;
    private static final float MINIMAL_SHARE = 0.35f;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null || mc.isPaused()) return;
        if (!level.dimension().equals(Level.END) || !ClientWeatherState.hasEffect(WeatherEffects.ENDER_PARTICLES)) return;

        float strength = ClientWeatherState.presence(WeatherTypes.ENDERSTORM, 1f);
        if (strength <= 0f) return;

        boolean minimal = mc.options.particles().get() == ParticleStatus.MINIMAL;
        float share = strength * (minimal ? MINIMAL_SHARE : 1f);

        ParticleRainDrops.Shower shower = ParticleRainDrops.shower();
        if (shower != null) {
            fallAsParticleRain(mc, level, shower, share);
        } else {
            fallAsStreaks(level, mc.player.position(), share, minimal);
        }
    }

    private static void fallAsParticleRain(Minecraft mc, ClientLevel level, ParticleRainDrops.Shower shower, float share) {
        RandomSource random = level.random;
        Vec3 camera = mc.gameRenderer.getMainCamera().getPosition();

        int drops = Math.round(shower.drops() * share);
        for (int i = 0; i < drops; i++) {
            Vec3 point = shower.point(random, camera);
            if (point.y <= level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(point.x), Mth.floor(point.z))) continue;

            ParticleRainDrops.fall(level, point.x, point.y, point.z, VoidDropParticle.pickTint(random), VoidDropParticle::land);
        }
    }

    private static void fallAsStreaks(ClientLevel level, Vec3 player, float share, boolean minimal) {
        RandomSource random = level.random;
        Vec3 prevailing = ClientWind.direction();

        int drops = Math.round(DROPS * share);
        for (int i = 0; i < drops; i++) {
            double x = player.x + (random.nextDouble() - 0.5) * 2 * RADIUS;
            double z = player.z + (random.nextDouble() - 0.5) * 2 * RADIUS;
            double y = player.y + HEIGHT + random.nextDouble() * HEIGHT_SPREAD;
            if (y <= level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(x), Mth.floor(z))) continue;

            Vec3 wind = WindField.sample(level, prevailing, x, y, z, 0f).velocity();
            level.addParticle(AWParticles.VOID_DROP.get(), minimal, x, y, z, wind.x, 0.0, wind.z);
        }
    }
}
