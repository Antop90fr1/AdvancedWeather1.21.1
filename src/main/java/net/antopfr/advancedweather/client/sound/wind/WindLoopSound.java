package net.antopfr.advancedweather.client.sound.wind;

import net.antopfr.advancedweather.client.wind.ClientWind;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

public class WindLoopSound extends AbstractTickableSoundInstance {

    public enum Layer { LIGHT, HEAVY }

    private final Layer layer;
    private float current;

    public WindLoopSound(SoundEvent sound, Layer layer) {
        super(sound, SoundSource.WEATHER, RandomSource.create());
        this.layer = layer;
        this.looping = true;
        this.delay = 0;
        this.relative = true;
        this.current = target();
        this.volume = Math.max(0.001f, current);
    }

    @Override
    public void tick() {
        float target = target();
        current += (target - current) * (target > current ? 0.06f : 0.025f);
        this.volume = Math.max(0.001f, current);
        this.pitch = layer == Layer.HEAVY
                ? 0.85f + ClientWind.gust() * ClientWind.intensity() * 0.25f
                : 1.0f + ClientWind.intensity() * 0.15f;
    }

    private float target() {
        float intensity = ClientWind.intensity();
        float exposure = ClientWind.exposure();

        if (layer == Layer.LIGHT) {
            return Mth.clamp(intensity * 0.8f, 0f, 0.8f) * (0.3f + 0.7f * exposure);
        }
        float t = Mth.clamp((intensity - 0.3f) / 0.6f, 0f, 1f);
        float strong = t * t * (3f - 2f * t);
        return strong * (0.3f + 0.7f * ClientWind.gust()) * (0.15f + 0.85f * exposure);
    }
}
