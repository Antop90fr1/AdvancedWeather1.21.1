package net.antopfr.advancedweather.client.storm;

import net.antopfr.advancedweather.AdvancedWeather;
import net.antopfr.advancedweather.config.AWClientConfig;
import net.antopfr.advancedweather.network.toclient.StormApproachPacket;
import net.antopfr.advancedweather.weather.StormApproach;
import net.antopfr.advancedweather.weather.WeatherTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = AdvancedWeather.MOD_ID, value = Dist.CLIENT)
public final class ComingWeather {

    private ComingWeather() {}

    private static long arrival = StormApproachPacket.NONE;
    private static WeatherTypes weather = WeatherTypes.CLEAR;

    private static long arrivedAt = StormApproachPacket.NONE;
    private static WeatherTypes arrived = WeatherTypes.CLEAR;

    public static void announce(long arrivalTick, WeatherTypes coming) {
        arrival = arrivalTick;
        weather = coming;
    }

    public static WeatherTypes weather() {
        return weather;
    }

    public static long ticksAway(WeatherTypes... types) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || arrival < 0 || !AWClientConfig.get().distantStorms) return -1;

        for (WeatherTypes type : types) {
            if (type != weather) continue;
            long away = arrival - level.getGameTime();
            return away > 0 && away <= StormApproach.lead(type) ? away : -1;
        }
        return -1;
    }

    public static float nearness(WeatherTypes type, float partialTick) {
        long away = ticksAway(type);
        if (away < 0) return 0f;
        return Math.min(1f, 1f - (away - partialTick) / StormApproach.lead(type));
    }

    public static long sinceArrival(WeatherTypes type) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || arrivedAt < 0 || arrived != type) return -1;
        return Math.max(0L, level.getGameTime() - arrivedAt);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            arrival = arrivedAt = StormApproachPacket.NONE;
            return;
        }
        if (arrival >= 0 && arrival <= level.getGameTime()) {
            arrivedAt = arrival;
            arrived = weather;
            arrival = StormApproachPacket.NONE;
        }
    }
}
