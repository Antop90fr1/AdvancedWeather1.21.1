package net.antopfr.advancedweather.client.state;

import net.antopfr.advancedweather.AdvancedWeather;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

@EventBusSubscriber(modid = AdvancedWeather.MOD_ID, value = Dist.CLIENT)
public final class WeatherlessDimension {

    private WeatherlessDimension() {}

    private static boolean off;

    public static boolean off() {
        return off;
    }

    public static void set(boolean weatherless) {
        off = weatherless;
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        off = false;
    }
}
