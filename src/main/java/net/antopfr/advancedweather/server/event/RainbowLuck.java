package net.antopfr.advancedweather.server.event;

import net.antopfr.advancedweather.AdvancedWeather;
import net.antopfr.advancedweather.config.AWCommonConfig;
import net.antopfr.advancedweather.weather.WeatherDefinitions;
import net.antopfr.advancedweather.weather.WeatherManager;
import net.antopfr.advancedweather.weather.effect.types.rainbows.Rainbow;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = AdvancedWeather.MOD_ID)
public final class RainbowLuck {

    private RainbowLuck() {}

    private static final int INTERVAL = 40;
    private static final int LUCK_TICKS = 100;
    private static final float SEEN = 0.2f;

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !AWCommonConfig.get().rainbowBringsLuck) return;
        ServerLevel level = player.serverLevel();
        if (level.getGameTime() % INTERVAL != 0 || !level.dimension().equals(Level.OVERWORLD)) return;

        WeatherManager manager = WeatherManager.get(level);
        if (WeatherDefinitions.get(manager.getCurrentWeather(level)).hidesSun) return;

        float shows = Rainbow.envelope(level.getGameTime(), manager.getRainbowStart(), manager.getRainbowEnd())
                * Rainbow.sunFactor(Rainbow.sunElevationDeg(level.getTimeOfDay(1f)));
        if (shows < SEEN || !level.canSeeSky(BlockPos.containing(player.getEyePosition()))) return;

        player.addEffect(new MobEffectInstance(MobEffects.LUCK, LUCK_TICKS, 0, true, false, true));
    }
}
