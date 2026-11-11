package net.antopfr.advancedweather.weather;

public enum CloudShape {

    FAIR_CUMULUS(3f, 10f, 0.75f, 0.9f),

    CUMULUS(4f, 15f, 0.65f, 1.0f),

    STRATUS(2f, 5f, 0.15f, 0.6f),

    NIMBOSTRATUS(5f, 10f, 0.25f, 0.7f),

    CUMULONIMBUS(8f, 34f, 0.85f, 1.9f);

    public final float minThickness;
    public final float maxThickness;
    public final float puff;
    public final float heightCurve;

    CloudShape(float minThickness, float maxThickness, float puff, float heightCurve) {
        this.minThickness = minThickness;
        this.maxThickness = maxThickness;
        this.puff = puff;
        this.heightCurve = heightCurve;
    }
}
