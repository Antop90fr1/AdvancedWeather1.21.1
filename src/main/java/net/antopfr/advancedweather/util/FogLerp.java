package net.antopfr.advancedweather.util;

import net.antopfr.advancedweather.client.wind.ClientWind;
import net.antopfr.advancedweather.weather.WeatherDefinitions;
import net.antopfr.advancedweather.weather.WeatherTypes;

public class FogLerp {
    private static final float BLIZZARD_GUST_SHARE = 0.5f;

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

        float prevNear = nearFor(prev, renderDistance);
        float prevFar  = farFor(prev, renderDistance);
        float currentNear = nearFor(current, renderDistance);
        float currentFar  = farFor(current, renderDistance);
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

    public static float nearFor(WeatherTypes type, float renderDistance) {
        float near = WeatherDefinitions.get(type).fogNear;
        return near < 0 ? renderDistance : near;
    }

    public static float farFor(WeatherTypes type, float renderDistance) {
        float far = WeatherDefinitions.get(type).fogFar;
        if (far < 0) return renderDistance;
        if (type == WeatherTypes.BLIZZARD) far *= 1f - BLIZZARD_GUST_SHARE * ClientWind.gust();
        return Math.min(renderDistance, far);
    }
}
