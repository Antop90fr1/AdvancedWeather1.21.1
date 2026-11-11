package net.antopfr.advancedweather.compat.catnip;

import net.minecraft.util.Mth;

public final class PlainSmoothed implements Smoothed {

    private double previous;
    private double current;
    private double target;
    private double speed = 0.5;

    @Override
    public void startWithValue(double value) {
        this.previous = this.current = this.target = value;
    }

    @Override
    public void chase(double target, double speed) {
        this.target = target;
        this.speed = Mth.clamp(speed, 0.0, 1.0);
    }

    @Override
    public void tickChaser() {
        this.previous = this.current;
        this.current += (this.target - this.current) * this.speed;
    }

    @Override
    public float getValue(float partialTick) {
        return (float) Mth.lerp(partialTick, this.previous, this.current);
    }
}
