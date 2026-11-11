package net.antopfr.advancedweather.client.sound.wind;

import net.antopfr.advancedweather.AdvancedWeather;
import net.antopfr.advancedweather.client.sound.AWSounds;
import net.antopfr.advancedweather.client.wind.ClientWind;
import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.SoundManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = AdvancedWeather.MOD_ID, value = Dist.CLIENT)
public final class WindSoundManager {

    private WindSoundManager() {}

    private static final float AUDIBLE_INTENSITY = 0.03f;

    private static WindLoopSound light;
    private static WindLoopSound heavy;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            stop(mc);
            return;
        }
        if (mc.isPaused()) return;

        if (ClientWind.intensity() < AUDIBLE_INTENSITY) {
            stop(mc);
            return;
        }

        SoundManager sounds = mc.getSoundManager();
        if (light == null || !sounds.isActive(light)) {
            light = new WindLoopSound(AWSounds.WIND_LIGHT.get(), WindLoopSound.Layer.LIGHT);
            sounds.play(light);
        }
        if (heavy == null || !sounds.isActive(heavy)) {
            heavy = new WindLoopSound(AWSounds.WIND_HEAVY.get(), WindLoopSound.Layer.HEAVY);
            sounds.play(heavy);
        }
    }

    public static void stop(Minecraft mc) {
        if (light != null) {
            mc.getSoundManager().stop(light);
            light = null;
        }
        if (heavy != null) {
            mc.getSoundManager().stop(heavy);
            heavy = null;
        }
    }
}
