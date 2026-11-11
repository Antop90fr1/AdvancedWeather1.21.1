package net.antopfr.advancedweather.weather.effect.types.chorus_plants;

import net.antopfr.advancedweather.content.entity.AWEntities;
import net.antopfr.advancedweather.weather.WeatherEffects;
import net.antopfr.advancedweather.weather.effect.EffectManager;
import net.antopfr.advancedweather.weather.wind.WindField;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

@EventBusSubscriber(modid = "advancedweather")
public class ChorusPlantHandler {

    private static int tickCounter = 0;
    private static final int SPAWN_INTERVAL = 80;
    private static final int MAX_PER_PLAYER = 6;

    @SubscribeEvent
    public static void onServerTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!level.dimension().equals(Level.END)) return;

        if (!EffectManager.get(level).hasEffect(WeatherEffects.CHORUS_PLANTS)) return;

        tickCounter++;
        if (tickCounter % SPAWN_INTERVAL != 0) return;

        for (ServerPlayer player : level.players()) {
            long existing = level.getEntitiesOfClass(
                    ChorusPlantEntity.class,
                    AABB.ofSize(player.position(), 96, 96, 96)).size();
            if (existing >= MAX_PER_PLAYER) continue;

            Vec3 wind = WindField.prevailingDirection(level, 0f);
            double spawnX = player.getX() - wind.x * 24 + (level.random.nextDouble() - 0.5) * 12;
            double spawnZ = player.getZ() - wind.z * 24 + (level.random.nextDouble() - 0.5) * 12;

            BlockPos ground = level.getHeightmapPos(
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    new BlockPos((int) spawnX, 0, (int) spawnZ));

            if (level.getBlockState(ground).liquid()) continue;
            if (ground.getY() <= level.getMinBuildHeight() + 1) continue;

            ChorusPlantEntity chorus = AWEntities.CHORUS_PLANT.get().create(level);
            if (chorus == null) continue;

            chorus.moveTo(spawnX, ground.getY() + 0.1, spawnZ, 0, 0);
            level.addFreshEntity(chorus);
        }
    }
}
