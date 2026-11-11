package net.antopfr.advancedweather.util;

import net.antopfr.advancedweather.weather.WeatherDefinitions;
import net.antopfr.advancedweather.weather.WeatherTypes;

public class FogLerp {
    private float currentNear;
    private float currentFar;
    private boolean shouldRenderCustomFog;

    public void update(WeatherTypes prev, WeatherTypes current, float progress, float renderDistance) {
        if (!prev.hasFog() && !current.hasFog()) {
            this.shouldRenderCustomFog = false;
            return;
        }

        if (progress >= 1.0f && !current.hasFog()) {
            this.shouldRenderCustomFog = false;
            return;
        }

        this.shouldRenderCustomFog = true;

        float prevNear = getNearDistanceFor(prev, renderDistance);
        float prevFar  = getFarDistanceFor(prev, renderDistance);
        float currentNear = getNearDistanceFor(current, renderDistance);
        float currentFar  = getFarDistanceFor(current, renderDistance);
        this.currentNear = prevNear + (currentNear - prevNear) * progress;
        this.currentFar  = prevFar + (currentFar - prevFar) * progress;
    }

    public boolean shouldRenderCustomFog() {
        return this.shouldRenderCustomFog;
    }
    public float getCurrentNear() {
        return this.currentNear;
    }
    public float getCurrentFar() {
        return this.currentFar;
    }

    private static float getNearDistanceFor(WeatherTypes type, float renderDistance) {
        float near = WeatherDefinitions.get(type).fogNear;
        return near < 0 ? renderDistance : near;
    }

    private static float getFarDistanceFor(WeatherTypes type, float renderDistance) {
        float far = WeatherDefinitions.get(type).fogFar;
        return far < 0 ? renderDistance : Math.min(renderDistance, far);
    }
}
