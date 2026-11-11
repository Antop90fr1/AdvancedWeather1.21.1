package net.antopfr.advancedweather.weather.effect.types.sand_particles;

import net.antopfr.advancedweather.client.state.ClientWeatherState;
import net.antopfr.advancedweather.config.AWClientConfig;
import net.antopfr.advancedweather.client.particle.AWParticles;
import net.antopfr.advancedweather.util.PlayerChecks;
import net.antopfr.advancedweather.weather.WeatherEffects;
import net.antopfr.advancedweather.client.wind.ClientWind;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = "advancedweather", value = Dist.CLIENT)
public class SandParticleHandler {
    private static int tickCounter = 0;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        AWClientConfig config = AWClientConfig.get();

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.isPaused()) return;
        if (!ClientWeatherState.hasEffect(WeatherEffects.SAND_PARTICLES)) return;
        if (PlayerChecks.isShielded(mc.level, mc.player.blockPosition())) return;

        tickCounter++;
        if (tickCounter % config.sandTickInterval != 0) return;

        blow(mc, 1f);
    }

    public static void blow(Minecraft mc, float share) {
        AWClientConfig config = AWClientConfig.get();
        Player player = mc.player;
        double px = player.getX();
        double py = player.getY();
        double pz = player.getZ();

        BlockPos playerPos = player.blockPosition();
        Holder<Biome> biomeHolder = mc.level.getBiome(playerPos);

        boolean isDesert = biomeHolder.is(Biomes.DESERT) || biomeHolder.is(Biomes.BEACH);
        boolean isBadlands = biomeHolder.is(BiomeTags.IS_BADLANDS);
        boolean isSoulSandValley = biomeHolder.is(Biomes.SOUL_SAND_VALLEY);

        ParticleOptions particle =
                isBadlands ? AWParticles.RED_SAND_PARTICLE.get()
                        : isSoulSandValley ? AWParticles.SOUL_SAND_PARTICLE.get()
                        : isDesert ? AWParticles.SAND_PARTICLE.get()
                        : null;

        Vec3 wind = ClientWind.direction();
        double surge = ClientWind.surge();
        double windX = wind.x;
        double windZ = wind.z;

        int count = Math.round(config.sandCount * share);
        float spawnDist = (float) config.sandSpawnDist;

        for (int i = 0; i < count; i++) {
            double ox = -windX * spawnDist + (mc.level.random.nextDouble() - 0.5) * config.sandSpread;
            double oy = (mc.level.random.nextDouble() * 2 - 0.5) * config.sandHeightSpread;
            double oz = -windZ * spawnDist + (mc.level.random.nextDouble() - 0.5) * config.sandSpread;

            double dx = wind.x * config.sandWindSpeed * surge;
            double dz = wind.z * config.sandWindSpeed * surge;
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
