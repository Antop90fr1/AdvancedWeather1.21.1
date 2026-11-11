package net.antopfr.advancedweather.weather.modifiers;

import net.antopfr.advancedweather.weather.WeatherTypes;

public class AtmosphereModifiers {

    public static float atmosphereModifier(WeatherTypes target, float tempC, float humidity) {
        return switch (target) {

            case FOG -> {
                float h = humidityFactor(humidity, 80f, 100f);
                float t = tempFactor(tempC, -5f, 15f);
                yield h * t;
            }
            case DENSE_FOG -> {
                float h = humidityFactor(humidity, 90f, 100f);
                float t = tempFactor(tempC, -2f, 10f);
                yield h * t;
            }
            case MIST -> {
                float h = humidityFactor(humidity, 70f, 100f);
                float t = tempFactor(tempC, 0f, 20f);
                yield h * t;
            }

            case SNOW -> {
                float t = tempC < 2f ? 2.5f : tempC < 5f ? 1.0f : 0.05f;
                float h = humidityFactor(humidity, 60f, 100f);
                yield t * h;
            }
            case BLIZZARD -> {
                float t = tempC < -3f ? 2.5f : tempC < 0f ? 1.0f : 0.02f;
                float h = humidityFactor(humidity, 65f, 100f);
                yield t * h;
            }
            case FREEZING_RAIN -> {
                float t = tempC > -2f && tempC < 3f ? 2.0f : 0.1f;
                yield t;
            }

            case THUNDERSTORM -> {
                float t = tempFactor(tempC, 10f, 40f);
                float h = humidityFactor(humidity, 70f, 100f);
                yield t * h;
            }

            case HEAVY_RAIN -> {
                float h = humidityFactor(humidity, 75f, 100f);
                yield h * liquidFactor(tempC);
            }

            case LIGHT_RAIN -> {
                float h = humidityFactor(humidity, 60f, 100f);
                yield h * liquidFactor(tempC);
            }
            case DRIZZLE -> {
                float h = humidityFactor(humidity, 55f, 90f);
                yield h * liquidFactor(tempC);
            }

            case SANDSTORM -> {
                float t = tempFactor(tempC, 25f, 55f);
                float h = humidity < 20f ? 2.0f : humidity < 35f ? 1.0f : 0.1f;
                yield t * h;
            }

            case CLEAR -> {
                float h = humidity < 50f ? 1.5f : humidity < 70f ? 1.0f : 0.6f;
                yield h;
            }
            case SUNNY -> {
                float t = tempFactor(tempC, 15f, 40f);
                float h = humidity < 45f ? 1.5f : 0.7f;
                yield t * h;
            }

            case HAIL -> {
                float t = tempC < 10f ? 0.4f : 1.5f;
                float h = humidityFactor(humidity, 70f, 100f);
                yield t * h;
            }

            case WINDY -> humidity < 50f ? 1.2f : 0.9f;

            case OVERCAST -> humidityFactor(humidity, 60f, 100f);

            case CLOUDY -> 1.0f;

            default -> 1.0f;
        };
    }

    public static float pressureModifier(WeatherTypes target, float p) {
        float inv = 1.0f - p;
        return switch (target) {
            case CLEAR, SUNNY -> p > 0.6f ? 0.2f + p * 3.5f : p * 0.4f;
            case WINDY       -> p > 0.7f ? 0.5f + p * 2.0f : inv > 0.7f ? 0.3f + inv * 2.0f : 0.3f;
            case MIST        -> 0.3f + p * 1.5f;
            case CLOUDY      -> 0.8f;
            case OVERCAST    -> 0.4f + inv * 2.0f;
            case FOG         -> 0.3f + inv * 1.5f;
            case DENSE_FOG   -> 0.2f + inv * 2.0f;
            case DRIZZLE     -> 0.1f + inv * 2.5f;
            case LIGHT_RAIN  -> 0.1f + inv * 2.8f;
            case SNOW        -> 0.05f + inv * 2.5f;
            case HEAVY_RAIN  -> 0.02f + inv * 4.0f;
            case BLIZZARD    -> 0.02f + inv * 3.5f;
            case FREEZING_RAIN -> 0.02f + inv * 3.0f;
            case SANDSTORM   -> 0.02f + inv * 3.0f;
            case THUNDERSTORM -> inv > 0.35f ? 0.02f + inv * 5.0f : 0.01f;
            case HAIL         -> inv > 0.4f ? 0.02f + inv * 4.0f : 0.01f;
            default -> 1.0f;
        };
    }

    private static float liquidFactor(float temp) {
        if (temp >= 3f) return 1.0f;
        if (temp <= -1f) return 0.03f;
        return 0.03f + (temp + 1f) / 4f * 0.97f;
    }

    private static float tempFactor(float temp, float optMin, float optMax) {
        if (temp >= optMin && temp <= optMax) {
            float center = (optMin + optMax) / 2f;
            float half   = (optMax - optMin) / 2f;
            float dist   = Math.abs(temp - center) / half;
            return 1.0f + (1.0f - dist);
        }
        float dist = temp < optMin ? optMin - temp : temp - optMax;
        return Math.max(0.1f, 1.0f - dist * 0.15f);
    }

    private static float humidityFactor(float humidity, float optMin, float optMax) {
        if (humidity >= optMin) {
            float range = optMax - optMin;
            float t = Math.min(1f, (humidity - optMin) / range);
            return 1.0f + t;
        }
        float dist = optMin - humidity;
        return Math.max(0.1f, 1.0f - dist * 0.05f);
    }
}
