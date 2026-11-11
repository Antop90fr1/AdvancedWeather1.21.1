package net.antopfr.advancedweather.weather.effect.types.void_storm;

import net.antopfr.advancedweather.client.sound.AWSounds;
import net.antopfr.advancedweather.client.state.ClientWeatherState;
import net.antopfr.advancedweather.client.wind.ClientWind;
import net.antopfr.advancedweather.weather.WeatherTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

public class VoidDroneSound extends AbstractTickableSoundInstance {

    private float current;

    public VoidDroneSound() {
        super(AWSounds.WIND_HEAVY.get(), SoundSource.WEATHER, RandomSource.create());
        this.looping = true;
        this.delay = 0;
        this.relative = true;
        this.pitch = 0.5f;
        this.volume = 0.001f;
    }

    @Override
    public void tick() {
        ClientLevel level = Minecraft.getInstance().level;
        float presence = level != null && level.dimension().equals(Level.END)
                ? ClientWeatherState.presence(WeatherTypes.VOID_STORM, 1f)
                : 0f;
        if (presence <= 0f) {
            this.stop();
            return;
        }

        float surge = VoidPulse.surge(level.getGameTime(), 1f);
        float target = presence * (0.1f + 0.5f * surge) * (0.3f + 0.7f * ClientWind.exposure());
        current += (target - current) * 0.2f;
        this.volume = Math.max(0.001f, current);
    }
}
