package net.antopfr.advancedweather.weather.wind;

import net.minecraft.world.phys.Vec3;

public record WindSample(Vec3 direction, float speedKmh, float gust) {

    public double blocksPerTick() {
        return WindScale.kmhToBlocksPerTick(speedKmh);
    }

    public Vec3 velocity() {
        return direction.scale(blocksPerTick());
    }

    public float intensity() {
        return Math.min(1.25f, WindScale.kmhToIntensity(speedKmh));
    }
}
