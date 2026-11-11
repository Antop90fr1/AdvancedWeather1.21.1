package net.antopfr.advancedweather.api;

public record AtmosphereSnapshot(
        float pressure,
        float pressureVelocity,
        float dewPoint,
        float temperature,
        float humidity,
        float windIntensity,
        String mode,
        String pressureCategory
) {}
