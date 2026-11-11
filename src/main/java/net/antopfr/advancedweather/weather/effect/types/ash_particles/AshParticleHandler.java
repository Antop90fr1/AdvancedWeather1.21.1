package net.antopfr.advancedweather.weather.effect.types.ash_particles;

import net.antopfr.advancedweather.client.particle.AWParticles;
import net.antopfr.advancedweather.client.state.ClientWeatherState;
import net.antopfr.advancedweather.config.AWClientConfig;
import net.antopfr.advancedweather.util.PlayerChecks;
import net.antopfr.advancedweather.weather.WeatherEffects;
import net.antopfr.advancedweather.client.wind.ClientWind;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = "advancedweather", value = Dist.CLIENT)
public class AshParticleHandler {
    private static int tickCounter = 0;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        AWClientConfig config = AWClientConfig.get();

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.isPaused()) return;
        if (!ClientWeatherState.hasEffect(WeatherEffects.ASH_PARTICLES)) return;
        if (PlayerChecks.isShielded(mc.level, mc.player.blockPosition())) return;

        tickCounter++;
        if (tickCounter % config.sandTickInterval != 0) return;

        Player player = mc.player;
        double px = player.getX();
        double py = player.getY();
        double pz = player.getZ();

        ParticleOptions particle = AWParticles.ASH_PARTICLE.get();

        Vec3 wind = ClientWind.direction();
        double surge = ClientWind.surge();
        double windX = wind.x;
        double windZ = wind.z;

        int count = 96;
        float spawnDist = (float) config.sandSpawnDist;

        for (int i = 0; i < count; i++) {
            double ox = -windX * spawnDist + (mc.level.random.nextDouble() - 0.5) * config.sandSpread;
            double oy = (mc.level.random.nextDouble() * 3 - 0.5) * config.sandHeightSpread;
            double oz = -windZ * spawnDist + (mc.level.random.nextDouble() - 0.5) * config.sandSpread;

            double dx = wind.x * config.sandWindSpeed * 0.8 * surge;
            double dz = wind.z * config.sandWindSpeed * 0.8 * surge;

            if (particle != null) {
                mc.level.addParticle(
                        particle,
                        px + ox, py + oy, pz + oz,
                        dx, 0, dz
                );
            }
        }
    }
}
