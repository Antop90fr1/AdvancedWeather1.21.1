package net.antopfr.advancedweather.server.event;

import net.antopfr.advancedweather.config.AWCommonConfig;
import net.antopfr.advancedweather.weather.SandDrift;
import net.antopfr.advancedweather.weather.WeatherManager;
import net.antopfr.advancedweather.weather.WeatherTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = "advancedweather")
public class SandstormEffect {

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!AWCommonConfig.get().sandstormAffectsPlayers) return;
        if (player.isCreative() || player.isSpectator()) return;

        ServerLevel level = player.serverLevel();
        if (!level.dimension().equals(Level.OVERWORLD)) return;
        if (WeatherManager.get(level).getCurrentWeather(level) != WeatherTypes.SANDSTORM) return;
        if (level.getGameTime() % 40 != 0) return;

        BlockPos pos = player.blockPosition();
        if (!level.canSeeSky(pos)) return;
        if (!isSandyBiome(level, pos)) return;

        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0, false, false, false));
        player.causeFoodExhaustion(0.5f);
    }

    public static boolean isSandyBiome(ServerLevel level, BlockPos pos) {
        return SandDrift.isSandy(level.getBiome(pos));
    }
}
