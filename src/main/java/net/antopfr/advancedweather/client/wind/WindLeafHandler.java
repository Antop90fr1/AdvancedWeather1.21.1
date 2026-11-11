package net.antopfr.advancedweather.client.wind;

import net.antopfr.advancedweather.AdvancedWeather;
import net.antopfr.advancedweather.client.particle.types.LeafParticle;
import net.antopfr.advancedweather.weather.wind.WindField;
import net.antopfr.advancedweather.weather.wind.WindSample;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = AdvancedWeather.MOD_ID, value = Dist.CLIENT)
public final class WindLeafHandler {

    private WindLeafHandler() {}

    private static final double RADIUS = 25.0;
    private static final int MAX_ATTEMPTS = 5;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        Player player = mc.player;
        if (level == null || player == null || mc.isPaused()) return;
        if (LeafParticle.Provider.CURRENT_SPRITES == null) return;

        float strength = ClientWind.visibility() / Math.max(0.05f, ClientWind.exposure());
        if (strength < 0.1f) return;

        RandomSource random = level.random;
        int attempts = Math.round(strength * MAX_ATTEMPTS);
        Vec3 prevailing = ClientWind.direction();

        for (int i = 0; i < attempts; i++) {
            double r = random.nextDouble() * RADIUS;
            double theta = random.nextDouble() * Math.PI * 2;
            int x = (int) Math.floor(player.getX() + r * Math.cos(theta));
            int z = (int) Math.floor(player.getZ() + r * Math.sin(theta));

            int top = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
            if (top <= level.getMinBuildHeight()) continue;

            BlockPos leafPos = new BlockPos(x, top - 1, z);
            BlockState state = level.getBlockState(leafPos);
            if (!state.is(BlockTags.LEAVES)) continue;

            int color = mc.getBlockColors().getColor(state, level, leafPos, 0);
            if (color == -1) color = level.getBiome(leafPos).value().getFoliageColor();

            double sx = x + random.nextDouble();
            double sy = leafPos.getY() - 0.1;
            double sz = z + random.nextDouble();

            WindSample air = WindField.sample(level, prevailing, sx, sy, sz, 0f);
            Vec3 drift = air.velocity().scale(0.35);

            mc.particleEngine.add(new LeafParticle(level, sx, sy, sz,
                    drift.x + (random.nextDouble() - 0.5) * 0.05,
                    -0.01 - random.nextDouble() * 0.02,
                    drift.z + (random.nextDouble() - 0.5) * 0.05,
                    color, LeafParticle.Provider.CURRENT_SPRITES));
        }
    }
}
