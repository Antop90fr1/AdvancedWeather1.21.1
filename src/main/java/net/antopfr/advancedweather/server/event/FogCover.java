package net.antopfr.advancedweather.server.event;

import net.antopfr.advancedweather.AdvancedWeather;
import net.antopfr.advancedweather.config.AWCommonConfig;
import net.antopfr.advancedweather.weather.ServerRain;
import net.antopfr.advancedweather.weather.WeatherTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.dimension.DimensionType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEvent;

@EventBusSubscriber(modid = AdvancedWeather.MOD_ID)
public final class FogCover {

    private FogCover() {}

    private static final double FOG_SIGHT = 0.6;
    private static final double DENSE_FOG_SIGHT = 0.4;

    private static final float DENSE_FOG_SPAWN = 0.2f;

    @SubscribeEvent
    public static void onVisibility(LivingEvent.LivingVisibilityEvent event) {
        if (!(event.getEntity() instanceof Player player) || !(event.getLookingEntity() instanceof Enemy)) return;
        if (!(player.level() instanceof ServerLevel level) || !AWCommonConfig.get().fogHidesPlayers) return;

        WeatherTypes weather = ServerRain.weatherOf(level);
        double sight = weather == WeatherTypes.FOG ? FOG_SIGHT
                : weather == WeatherTypes.DENSE_FOG ? DENSE_FOG_SIGHT
                : 1.0;
        if (sight < 1.0 && level.canSeeSky(BlockPos.containing(player.getEyePosition()))) {
            event.modifyVisibility(sight);
        }
    }

    public static boolean spawnsInFog(ServerLevelAccessor level, BlockPos pos, RandomSource random) {
        if (!AWCommonConfig.get().denseFogSpawnsMonsters) return false;
        if (ServerRain.weatherOf(level.getLevel()) != WeatherTypes.DENSE_FOG) return false;

        DimensionType dimension = level.dimensionType();
        int limit = dimension.monsterSpawnBlockLightLimit();
        if (limit < 15 && level.getBrightness(LightLayer.BLOCK, pos) > limit) return false;

        return random.nextFloat() < DENSE_FOG_SPAWN;
    }
}
