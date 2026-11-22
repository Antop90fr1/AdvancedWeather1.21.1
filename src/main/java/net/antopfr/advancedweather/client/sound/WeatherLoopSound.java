package net.antopfr.advancedweather.client.sound;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

import java.util.function.DoubleSupplier;

public class WeatherLoopSound extends AbstractTickableSoundInstance {

    private static final float FADE = 0.02f;
    private static final float SILENT = 0.005f;

    private final DoubleSupplier target;
    private float current;

    public WeatherLoopSound(SoundEvent sound, DoubleSupplier target) {
        super(sound, SoundSource.WEATHER, RandomSource.create());
        this.target = target;
        this.looping = true;
        this.delay = 0;
        this.relative = true;
        this.volume = 0.001f;
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public void tick() {
        float wanted = (float) target.getAsDouble();
        current += (wanted - current) * FADE;
        if (wanted <= 0f && current < SILENT) {
            stop();
            return;
        }
        volume = Math.max(0.001f, current);
    }
}
