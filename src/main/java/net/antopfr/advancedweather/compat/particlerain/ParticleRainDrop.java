package net.antopfr.advancedweather.compat.particlerain;

import net.minecraft.client.multiplayer.ClientLevel;

public interface ParticleRainDrop {

    void advancedweather$onLanding(Landing landing);

    @FunctionalInterface
    interface Landing {
        void land(ClientLevel level, double x, double y, double z);
    }
}
