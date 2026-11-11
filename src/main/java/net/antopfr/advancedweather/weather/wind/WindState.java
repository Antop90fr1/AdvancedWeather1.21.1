package net.antopfr.advancedweather.weather.wind;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class WindState {

    private static final float UPPER_MIN_KMH = 6f;
    private static final float UPPER_FACTOR = 0.45f;

    private static final double CORRECTION_RATE = 0.1;

    private static final double SNAP_DISTANCE = 96.0;

    private float intensity;
    private float syncedIntensity;
    private boolean synced;

    private double driftX, driftZ, prevDriftX, prevDriftZ;
    private double upperX, upperZ, prevUpperX, prevUpperZ;
    private double errorX, errorZ, errorUpperX, errorUpperZ;

    public void advance(Vec3 direction) {
        prevDriftX = driftX;
        prevDriftZ = driftZ;
        prevUpperX = upperX;
        prevUpperZ = upperZ;

        float kmh = WindScale.intensityToKmh(intensity);
        double surface = WindScale.kmhToBlocksPerTick(kmh);
        double upper = WindScale.kmhToBlocksPerTick(UPPER_MIN_KMH + kmh * UPPER_FACTOR);

        driftX += direction.x * surface;
        driftZ += direction.z * surface;
        upperX += direction.x * upper;
        upperZ += direction.z * upper;
    }

    public void approachIntensity(float target, float rate) {
        intensity += (target - intensity) * rate;
    }

    // CLIENT

    public void tickClient(Vec3 direction) {
        intensity = Mth.lerp(0.1f, intensity, syncedIntensity);
        advance(direction);

        driftX += errorX * CORRECTION_RATE;
        driftZ += errorZ * CORRECTION_RATE;
        upperX += errorUpperX * CORRECTION_RATE;
        upperZ += errorUpperZ * CORRECTION_RATE;
        errorX *= 1 - CORRECTION_RATE;
        errorZ *= 1 - CORRECTION_RATE;
        errorUpperX *= 1 - CORRECTION_RATE;
        errorUpperZ *= 1 - CORRECTION_RATE;
    }

    public void applySync(float intensity, double driftX, double driftZ, double upperX, double upperZ) {
        this.syncedIntensity = intensity;

        boolean far = Math.abs(driftX - this.driftX) > SNAP_DISTANCE
                || Math.abs(driftZ - this.driftZ) > SNAP_DISTANCE
                || Math.abs(upperX - this.upperX) > SNAP_DISTANCE
                || Math.abs(upperZ - this.upperZ) > SNAP_DISTANCE;

        if (!synced || far) {
            this.intensity = intensity;
            this.driftX = this.prevDriftX = driftX;
            this.driftZ = this.prevDriftZ = driftZ;
            this.upperX = this.prevUpperX = upperX;
            this.upperZ = this.prevUpperZ = upperZ;
            errorX = errorZ = errorUpperX = errorUpperZ = 0;
            synced = true;
            return;
        }

        errorX = driftX - this.driftX;
        errorZ = driftZ - this.driftZ;
        errorUpperX = upperX - this.upperX;
        errorUpperZ = upperZ - this.upperZ;
    }

    public void reset() {
        intensity = syncedIntensity = 0f;
        driftX = driftZ = prevDriftX = prevDriftZ = 0;
        upperX = upperZ = prevUpperX = prevUpperZ = 0;
        errorX = errorZ = errorUpperX = errorUpperZ = 0;
        synced = false;
    }

    public float intensity() { return intensity; }

    public double driftX(float partialTick) { return Mth.lerp(partialTick, prevDriftX, driftX); }
    public double driftZ(float partialTick) { return Mth.lerp(partialTick, prevDriftZ, driftZ); }
    public double upperX(float partialTick) { return Mth.lerp(partialTick, prevUpperX, upperX); }
    public double upperZ(float partialTick) { return Mth.lerp(partialTick, prevUpperZ, upperZ); }

    public void save(CompoundTag tag) {
        tag.putFloat("windIntensity", intensity);
        tag.putDouble("windDriftX", driftX);
        tag.putDouble("windDriftZ", driftZ);
        tag.putDouble("windUpperX", upperX);
        tag.putDouble("windUpperZ", upperZ);
    }

    public void load(CompoundTag tag) {
        intensity = tag.getFloat("windIntensity");
        driftX = prevDriftX = tag.getDouble("windDriftX");
        driftZ = prevDriftZ = tag.getDouble("windDriftZ");
        upperX = prevUpperX = tag.getDouble("windUpperX");
        upperZ = prevUpperZ = tag.getDouble("windUpperZ");
    }
}
