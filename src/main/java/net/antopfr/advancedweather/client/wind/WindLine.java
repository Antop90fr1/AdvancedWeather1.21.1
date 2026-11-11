package net.antopfr.advancedweather.client.wind;

import net.minecraft.world.phys.Vec3;

record WindLine(Vec3 start, Vec3 direction, double speed, float length, float opacity,
                long birthTick, int lifetime) {

    Vec3 positionAt(double age) {
        return start.add(direction.scale(speed * age));
    }

    boolean expired(long now) {
        return now - birthTick > lifetime;
    }
}
