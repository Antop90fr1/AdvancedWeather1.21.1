package net.antopfr.advancedweather.api;

import net.antopfr.advancedweather.weather.WeatherTypes;

public record WeatherForecast(
        WeatherTypes predictedNext,
        float confidenceNext,
        WeatherTypes predictedIn30Min,
        float confidenceIn30Min
) {}
