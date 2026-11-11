package net.antopfr.advancedweather.weather.effect.types.brimstone_particles;

import net.antopfr.advancedweather.client.particle.AWParticles;
import net.antopfr.advancedweather.client.state.ClientWeatherState;
import net.antopfr.advancedweather.config.AWClientConfig;
import net.antopfr.advancedweather.util.PlayerChecks;
import net.antopfr.advancedweather.weather.WeatherEffects;
import net.antopfr.advancedweather.client.wind.ClientWind;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = "advancedweather", value = Dist.CLIENT)
public class BrimstoneParticleHandler {
    private static int tickCounter = 0;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        AWClientConfig config = AWClientConfig.get();


        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.isPaused()) return;
        if (!ClientWeatherState.hasEffect(WeatherEffects.BRIMSTONE_PARTICLES)) return;
        if (PlayerChecks.isShielded(mc.level, mc.player.blockPosition())) return;

        tickCounter++;
        if (tickCounter % config.brimstoneTickInterval != 0) return;

        Player player = mc.player;
        double px = player.getX();
        double py = player.getY() + 1.0;
        double pz = player.getZ();

        Vec3 wind = ClientWind.direction();
        double surge = ClientWind.surge();
        double windX = -wind.x;
        double windZ = -wind.z;

        int count = config.brimstoneCount;
        float spawnDist = (float) config.brimstoneSpawnDist;

        for (int i = 0; i < count; i++) {
            double ox = windX * spawnDist + (mc.level.random.nextDouble() - 0.5) * config.brimstoneSpread;
            double oy = mc.level.random.nextDouble() * config.brimstoneHeightSpread - 2;
            double oz = windZ * spawnDist + (mc.level.random.nextDouble() - 0.5) * config.brimstoneSpread;

            double dx = -windX * config.brimstoneWindSpeed * surge + (mc.level.random.nextDouble() - 0.5) * 0.02;
            double dy = -0.005 + (mc.level.random.nextDouble() - 0.5) * 0.005;
            double dz = -windZ * config.brimstoneWindSpeed * surge + (mc.level.random.nextDouble() - 0.5) * 0.02;

            mc.level.addParticle(
                    AWParticles.BRIMSTONE_DUST.get(),
                    px + ox, py + oy, pz + oz,
                    dx, dy, dz
            );
        }
    }
}
