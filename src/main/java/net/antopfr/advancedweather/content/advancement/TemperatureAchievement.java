package net.antopfr.advancedweather.content.advancement;

import net.antopfr.advancedweather.AdvancedWeather;
import net.antopfr.advancedweather.weather.LocalAtmosphere;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = AdvancedWeather.MOD_ID)
public final class TemperatureAchievement {

    private TemperatureAchievement() {}

    private static final int INTERVAL = 20;
    private static final float COLDEST = -65.5f;
    private static final float HOTTEST = 144f;

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !player.onGround()) return;
        ServerLevel level = player.serverLevel();
        if (level.getGameTime() % INTERVAL != 0) return;

        if (level.dimension().equals(Level.NETHER)) {
            if (LocalAtmosphere.getLocalTemperature(level, player.blockPosition()) >= HOTTEST) {
                AWAdvancements.grant(player, "hottest");
            }
        } else if (level.dimension().equals(Level.END)) {
            if (LocalAtmosphere.getLocalTemperature(level, player.blockPosition()) <= COLDEST) {
                AWAdvancements.grant(player, "coldest");
            }
        }
    }
}
