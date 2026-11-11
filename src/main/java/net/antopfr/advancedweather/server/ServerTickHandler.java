package net.antopfr.advancedweather.server;

import net.antopfr.advancedweather.api.WeatherForecast;
import net.antopfr.advancedweather.api.external.OpenMeteo;
import net.antopfr.advancedweather.config.AWCommonConfig;
import net.antopfr.advancedweather.content.advancement.AWAdvancementEvents;
import net.antopfr.advancedweather.network.toclient.*;
import net.antopfr.advancedweather.weather.effect.EffectManager;
import net.antopfr.advancedweather.weather.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ServerLevelData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;


@EventBusSubscriber(modid = "advancedweather")
public class ServerTickHandler {

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        WeatherManager manager = WeatherManager.get(level);
        manager.tick(level);
        ServerRain.update(level, manager.getCurrentWeather(level));
    }

    @SubscribeEvent
    public static void onServerStopped(net.neoforged.neoforge.event.server.ServerStoppedEvent event) {
        ServerRain.clear();
    }

    @SubscribeEvent
    public static void onVanillaWeatherDisable(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            if (serverLevel.getLevelData() instanceof ServerLevelData serverData) {

                if (serverData.isRaining()) {
                    serverData.setRaining(false);
                }
                if (serverData.isThundering()) {
                    serverData.setThundering(false);
                }

                serverData.setRainTime(24000);
                serverData.setThunderTime(24000);
                serverData.setClearWeatherTime(24000);
            }
        }
    }

    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        AWCommonConfig config = AWCommonConfig.get();
        WeatherManager manager = WeatherManager.get(level);

        if (config.realWeatherEnabled && level.dimension().equals(Level.OVERWORLD)) {
            OpenMeteo.fetchAsync(config.latitude, config.longitude).thenAccept(type -> {
                if (type == null) return;
                level.getServer().execute(() -> manager.applyRealWeather(level, type));
            });
        } else {
            manager.startAutoWeather(level);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        syncAllToPlayer(player);
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        syncAllToPlayer(player);
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        syncAllToPlayer(player);
    }

    private static void syncAllToPlayer(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        WeatherManager manager = WeatherManager.get(level);
        AtmosphericSystem a = manager.getAtmosphere(level);
        WeatherTypes current = manager.getCurrentWeather(level);
        long time = level.getGameTime();

        AWAdvancementEvents.weatherExperienced(player, current);

        AtmosphericSystem forecast = a.forecastState(30, time, level, current);
        WeatherForecast outlook = manager.getForecast(level);

        PacketDistributor.sendToPlayer(player, new WeatherSyncPacket(current));
        PacketDistributor.sendToPlayer(player, new EffectSyncPacket(EffectManager.get(level).getActiveEffects()));
        PacketDistributor.sendToPlayer(player, new HistorySyncPacket(manager.getHistory(level).getEntries()));
        PacketDistributor.sendToPlayer(player, new PressureSyncPacket(
                a.getPressure(), a.getPressureVel(), forecast.getPressure(),
                a.getCategory(level).name(),
                outlook.predictedNext().name(), outlook.predictedIn30Min().name(),
                manager.getMode(level).name(),
                outlook.confidenceNext(), outlook.confidenceIn30Min()));
        PacketDistributor.sendToPlayer(player, WindSyncPacket.of(manager.getWindState(level)));
        PacketDistributor.sendToPlayer(player, new RainbowSyncPacket(manager.getRainbowStart(), manager.getRainbowEnd()));
        manager.syncGroundCover(player, true);
        manager.syncStormApproach(player);
        PacketDistributor.sendToPlayer(player, new TemperatureSyncPacket(
                a.getTemperature(), a.forecastTemperatureFromState(forecast, 30, time, current, level)));
        PacketDistributor.sendToPlayer(player, new HumiditySyncPacket(
                a.getHumidity(), a.forecastHumidityFromState(forecast, current, 30, time, level)));
    }
}
