package net.antopfr.advancedweather.compat.catnip;

import net.createmod.catnip.animation.LerpedFloat;

final class CatnipSmoothed implements Smoothed {

    private final LerpedFloat delegate = LerpedFloat.linear();

    @Override
    public void startWithValue(double value) {
        this.delegate.startWithValue(value);
    }

    @Override
    public void chase(double target, double speed) {
        this.delegate.chase(target, speed, LerpedFloat.Chaser.EXP);
    }

    @Override
    public void tickChaser() {
        this.delegate.tickChaser();
    }

    @Override
    public float getValue(float partialTick) {
        return this.delegate.getValue(partialTick);
    }
}
