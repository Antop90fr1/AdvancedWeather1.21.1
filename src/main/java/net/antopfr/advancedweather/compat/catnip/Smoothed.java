package net.antopfr.advancedweather.compat.catnip;

public interface Smoothed {
    void startWithValue(double value);

    void chase(double target, double speed);

    void tickChaser();

    float getValue(float partialTick);
}
