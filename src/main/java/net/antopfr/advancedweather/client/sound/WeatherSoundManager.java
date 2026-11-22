package net.antopfr.advancedweather.client.sound;

import net.antopfr.advancedweather.AdvancedWeather;
import net.antopfr.advancedweather.client.state.ClientWeatherState;
import net.antopfr.advancedweather.client.wind.ClientWind;
import net.antopfr.advancedweather.weather.WeatherEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

@EventBusSubscriber(modid = AdvancedWeather.MOD_ID, value = Dist.CLIENT)
public final class WeatherSoundManager {

    private WeatherSoundManager() {}

    private enum Bed {
        BLIZZARD(AWSounds.BLIZZARD, WeatherEffects.BLIZZARD_PARTICLES),
        SANDSTORM(AWSounds.SANDSTORM, WeatherEffects.SAND_PARTICLES);

        private final Supplier<SoundEvent> sound;
        private final WeatherEffects effect;

        Bed(Supplier<SoundEvent> sound, WeatherEffects effect) {
            this.sound = sound;
            this.effect = effect;
        }
    }

    private static final float AUDIBLE = 0.01f;
    private static final float SHELTERED = 0.2f;

    private static final Map<Bed, WeatherLoopSound> playing = new EnumMap<>(Bed.class);

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            stop(mc);
            return;
        }
        if (mc.isPaused()) return;

        SoundManager sounds = mc.getSoundManager();
        for (Bed bed : Bed.values()) {
            if (target(bed) < AUDIBLE) continue;
            WeatherLoopSound loop = playing.get(bed);
            if (loop == null || !sounds.isActive(loop)) {
                loop = new WeatherLoopSound(bed.sound.get(), () -> target(bed));
                playing.put(bed, loop);
                sounds.play(loop);
            }
        }
    }

    private static float target(Bed bed) {
        if (!ClientWeatherState.hasEffect(bed.effect)) return 0f;
        return SHELTERED + (1f - SHELTERED) * ClientWind.exposure();
    }

    private static void stop(Minecraft mc) {
        for (WeatherLoopSound loop : playing.values()) mc.getSoundManager().stop(loop);
        playing.clear();
    }
}
