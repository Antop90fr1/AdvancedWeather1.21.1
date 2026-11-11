package net.antopfr.advancedweather.weather.effect.types.ground_fog;

import net.antopfr.advancedweather.client.state.ClientWeatherState;
import net.antopfr.advancedweather.client.particle.AWParticles;
import net.antopfr.advancedweather.weather.WeatherEffects;
import net.antopfr.advancedweather.client.wind.ClientWind;
import net.antopfr.advancedweather.weather.wind.WindField;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.synth.PerlinSimplexNoise;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.List;

@EventBusSubscriber(modid = "advancedweather", value = Dist.CLIENT)
public class GroundFogHandler {

    private static final PerlinSimplexNoise NOISE = new PerlinSimplexNoise(
            RandomSource.create(42L),
            List.of(0)
    );

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        boolean isActive = ClientWeatherState.hasEffect(WeatherEffects.GROUND_FOG) || ClientWeatherState.hasEffect(WeatherEffects.END_GROUND_FOG);
        if (!isActive || mc.isPaused()) return;

        if (mc.level.getGameTime() % 2 != 0) return;

        Player player = mc.player;
        Vec3 windDir = ClientWind.direction();

        double noiseMoveX = -WindField.clientState().upperX(0f) * 0.4;
        double noiseMoveZ = -WindField.clientState().upperZ(0f) * 0.4;

        double biasX = -windDir.x * 14.0;
        double biasZ = -windDir.z * 14.0;
        double centerX = player.getX() + biasX;
        double centerZ = player.getZ() + biasZ;

        for (int i = 0; i < 12; i++) {
            double r = 2.0 + mc.level.random.nextDouble() * 26.0;
            double theta = mc.level.random.nextDouble() * Math.PI * 2;

            double x = centerX + r * Math.cos(theta);
            double z = centerZ + r * Math.sin(theta);

            double scale = 0.04;

            double noiseValue = NOISE.getValue(
                    (x + noiseMoveX) * scale,
                    (z + noiseMoveZ) * scale,
                    true
            );

            if (noiseValue < 0.25) {
                continue;
            }

            int groundY = mc.level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) x, (int) z);
            if (groundY <= mc.level.getMinBuildHeight()) continue;

            BlockPos groundPos = new BlockPos((int) x, groundY - 1, (int) z);
            BlockState underState = mc.level.getBlockState(groundPos);

            if (underState.isAir() || underState.is(Blocks.OAK_LEAVES) || underState.is(Blocks.SPRUCE_LEAVES)
                    || underState.is(Blocks.BIRCH_LEAVES) || underState.is(Blocks.JUNGLE_LEAVES) || underState.is(Blocks.ACACIA_LEAVES)
                    || underState.is(Blocks.CHERRY_LEAVES) || underState.is(Blocks.DARK_OAK_LEAVES) || underState.is(Blocks.MANGROVE_LEAVES) || underState.is(Blocks.FLOWERING_AZALEA_LEAVES)) {
                continue;
            }

            double spawnY = groundY + 0.01 + mc.level.random.nextDouble() * 0.15;

            double speedMultiplier = 0.002 + mc.level.random.nextDouble() * 0.002;
            double windX = windDir.x * speedMultiplier + (mc.level.random.nextDouble() - 0.5) * 0.0008;
            double windZ = windDir.z * speedMultiplier + (mc.level.random.nextDouble() - 0.5) * 0.0008;
            double windY = (mc.level.random.nextDouble() - 0.5) * 0.0003;

            mc.level.addParticle(
                    AWParticles.GROUND_FOG.get(),
                    x, spawnY, z,
                    windX, windY, windZ
            );
        }
    }
}