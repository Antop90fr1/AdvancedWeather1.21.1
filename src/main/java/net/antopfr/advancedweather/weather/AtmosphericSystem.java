package net.antopfr.advancedweather.weather;

import net.antopfr.advancedweather.compat.sereneseasons.SeasonModifiers;
import net.antopfr.advancedweather.util.OpenSimplex2;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public class AtmosphericSystem {

    public static final float P_MIN     = 950f;
    public static final float P_MAX     = 1050f;
    private static final float P_NOMINAL = 1013f;

    private static final float NO_BOUND_MIN = -Float.MAX_VALUE;
    private static final float NO_BOUND_MAX =  Float.MAX_VALUE;

    private static final long  SEED_P    = 0x4156L;
    private static final long  SEED_DP   = 0x9A3BL;

    private static final float THERMAL_RATE = 0.008f;

    private static final float MAX_FORCED_PRESSURE  = 30f;
    private static final float MAX_FORCED_DEW_POINT = 12f;
    private static final float MAX_FORCED_TEMP      = 16f;

    private float pressure    = P_NOMINAL;
    private float pressureVel = 0f;
    private float dewPoint    = 8f;
    private float dewPointVel = 0f;
    private float airTemp     = Float.NaN;

    public enum Mode { PROCEDURAL, REAL, MANUAL }
    private Mode  mode               = Mode.PROCEDURAL;
    private float softPMin  = NO_BOUND_MIN;
    private float softPMax  = NO_BOUND_MAX;
    private float softDPMin = NO_BOUND_MIN;
    private float softDPMax = NO_BOUND_MAX;
    private int   manualTicksRemaining = 0;

    private float lastTemperature = 15f;
    private float lastWeatherTemperature = 15f;
    private float lastHumidity    = 60f;
    private float lastWindIntensity = 0f;
    private WeatherTypes lastWeather = WeatherTypes.CLEAR;

    private final List<AtmosphericForcing> activeForcings = new ArrayList<>();

    private record Forced(float pressure, float dewPoint, float temperature) {}

    private static long getDimensionSeedOffset(Level level) {
        if (level.dimension().equals(Level.NETHER)) return 5000L;
        if (level.dimension().equals(Level.END)) return 10000L;
        return 0L;
    }

    public void tick(ServerLevel level, WeatherTypes weatherType) {
        DimensionProfile profile = DimensionProfile.of(level);
        long dimensionOffset = getDimensionSeedOffset(level);
        long gameTime = level.getGameTime();
        long tod = level.getDayTime() % 24000;

        Forced forced = forced(0);
        activeForcings.removeIf(f -> !f.tick());
        lastWeather = weatherType;

        // PRESSURE
        float pTarget = Mth.clamp(pressureTarget(profile, dimensionOffset, gameTime) + forced.pressure(),
                profile.pMin, profile.pMax);

        pressureVel += (pTarget - pressure) * 0.008f;
        pressureVel *= 0.91f;
        pressureVel  = Mth.clamp(pressureVel, -2.0f, 2.0f);
        pressure     = Mth.clamp(pressure + pressureVel, profile.pMin, profile.pMax);

        // TEMPERATURE
        float airTarget = baseTemperature(weatherType, level) + forced.temperature();
        airTemp = Float.isNaN(airTemp) ? airTarget : airTemp + (airTarget - airTemp) * THERMAL_RATE;
        lastTemperature = temperatureOf(profile, airTemp, dewPoint, pressure, tod);
        float season = airTarget - (WeatherDefinitions.get(weatherType).tempBase + forced.temperature());
        lastWeatherTemperature = lastTemperature - solarEffect(profile, dewPoint, tod) - season;

        // DEW POINT
        float dpTarget = dewPointTarget(profile, dimensionOffset, gameTime, pressure) + forced.dewPoint();
        dpTarget = Mth.clamp(Math.min(dpTarget, lastTemperature), profile.dpMin, profile.dpMax);

        dewPointVel += (dpTarget - dewPoint) * 0.005f;
        dewPointVel *= 0.94f;
        dewPointVel  = Mth.clamp(dewPointVel, -0.3f, 0.3f);
        dewPoint     = Mth.clamp(dewPoint + dewPointVel, profile.dpMin, profile.dpMax);

        // HUMIDITY
        lastHumidity = Mth.clamp(computeRelativeHumidity(lastTemperature, dewPoint), 0f, 100f);

        // WIND
        float pressureGradient = Math.abs(pressureVel);
        float baseWindFromWeather = WeatherDefinitions.get(weatherType).windBase;
        float gradientWind = Mth.clamp(pressureGradient / 0.8f, 0f, 1f);

        float rawWindBase = Mth.clamp(Math.max(baseWindFromWeather, gradientWind * 0.75f + baseWindFromWeather * 0.25f), 0f, 1f);

        lastWindIntensity = rawWindBase;

        if (mode == Mode.MANUAL) {
            manualTicksRemaining--;
            if (manualTicksRemaining <= 0) mode = Mode.PROCEDURAL;
        }
    }

    private float pressureTarget(DimensionProfile profile, long dimensionOffset, long gameTime) {
        double t = gameTime / 96000.0;
        float macroNoise = OpenSimplex2.noise2(SEED_P + dimensionOffset, t * 0.15, t * 0.3);
        float microNoise = OpenSimplex2.noise2(SEED_P + dimensionOffset + 42L, t * 0.8, t * 1.5);
        float target = profile.pNominal + (macroNoise * 22.0f) + (microNoise * 6.0f);
        return mode == Mode.PROCEDURAL ? target : Mth.clamp(target, softPMin, softPMax);
    }

    private float dewPointTarget(DimensionProfile profile, long dimensionOffset, long gameTime, float atPressure) {
        double t = gameTime / 96000.0;
        float pNorm = (atPressure - profile.pMin) / (profile.pMax - profile.pMin);
        float dpFromPressure = Mth.lerp(pNorm, profile.dpMax * 0.65f, profile.dpMin * 0.5f);
        float dpNoise = OpenSimplex2.noise2(SEED_DP + dimensionOffset, t * 0.5, t * 0.9) * 6.0f;
        float target = dpFromPressure + dpNoise;
        return mode == Mode.PROCEDURAL ? target : Mth.clamp(target, softDPMin, softDPMax);
    }

    private static float baseTemperature(WeatherTypes weatherType, ServerLevel level) {
        float seasonOffset = SeasonModifiers.isSeasonalType(weatherType)
                ? 0f
                : SeasonModifiers.temperatureOffset(level);
        float base = WeatherDefinitions.get(weatherType).tempBase + seasonOffset;
        return fallsAsRain(weatherType) ? Math.max(base, RAIN_FLOOR) : base;
    }

    private static final float RAIN_FLOOR = 3f;

    private static boolean fallsAsRain(WeatherTypes type) {
        return type == WeatherTypes.DRIZZLE || type == WeatherTypes.LIGHT_RAIN
                || type == WeatherTypes.HEAVY_RAIN || type == WeatherTypes.THUNDERSTORM;
    }

    private static float temperatureOf(DimensionProfile profile, float air, float dewPointC, float atPressure, long tod) {
        float pNorm = (atPressure - profile.pMin) / (profile.pMax - profile.pMin);
        float adiabaticEffect = (pNorm - 0.5f) * 4f;
        return Mth.clamp(air + solarEffect(profile, dewPointC, tod) + adiabaticEffect, profile.tMin, profile.tMax);
    }

    private static float solarEffect(DimensionProfile profile, float dewPointC, long tod) {
        return profile.hasDayCycle
                ? (float) Math.cos((tod - 6000) * Math.PI / 12000.0) * computeSolarAmplitude(dewPointC, profile)
                : 0f;
    }

    private static float computeRelativeHumidity(float tempC, float dewPointC) {
        double eT  = Math.exp(17.27 * tempC    / (tempC    + 237.3));
        double eTd = Math.exp(17.27 * dewPointC / (dewPointC + 237.3));
        return (float)(100.0 * eTd / eT);
    }

    private static float computeSolarAmplitude(float dewPointC, DimensionProfile profile) {
        float normalized = Mth.clamp((dewPointC - profile.dpMin) / (profile.dpMax - profile.dpMin), 0f, 1f);
        return Mth.lerp(normalized, 8f, 3f);
    }

    // ALL MODES

    public void setProceduralMode() {
        mode = Mode.PROCEDURAL;
        softPMin = NO_BOUND_MIN; softPMax = NO_BOUND_MAX;
        softDPMin = NO_BOUND_MIN; softDPMax = NO_BOUND_MAX;
    }

    public void setManualMode(WeatherTypes type, ServerLevel level) {
        mode = Mode.MANUAL;
        manualTicksRemaining = 12000;
        applyWeatherConstraints(type, level);
        pressure = WeatherDefinitions.get(type).recommendedPressure;
        pressureVel = 0f;
        dewPoint = getRecommendedDewPoint(type, level);
        dewPointVel = 0f;
        airTemp = Float.NaN;
    }

    public void setRealWeatherMode(WeatherTypes type, ServerLevel level) {
        mode = Mode.REAL;
        applyWeatherConstraints(type, level);
        float targetP = WeatherDefinitions.get(type).recommendedPressure;
        float targetDP = getRecommendedDewPoint(type, level);
        if (pressure < softPMin || pressure > softPMax)
            pressureVel += (targetP - pressure) * 0.05f;
        if (dewPoint < softDPMin || dewPoint > softDPMax)
            dewPointVel += (targetDP - dewPoint) * 0.05f;
    }

    private void applyWeatherConstraints(WeatherTypes type, ServerLevel level) {
        DimensionProfile profile = DimensionProfile.of(level);
        float pCenter = WeatherDefinitions.get(type).recommendedPressure;
        float dpCenter = getRecommendedDewPoint(type, level);
        softPMin = Mth.clamp(pCenter - 15f, profile.pMin, profile.pMax);
        softPMax = Mth.clamp(pCenter + 15f, profile.pMin, profile.pMax);
        softDPMin = Mth.clamp(dpCenter - 4f, profile.dpMin, profile.dpMax);
        softDPMax = Mth.clamp(dpCenter + 4f, profile.dpMin, profile.dpMax);
    }

    public void snapToTarget(ServerLevel level, WeatherTypes type) {
        DimensionProfile profile = DimensionProfile.of(level);
        long dimensionOffset = getDimensionSeedOffset(level);
        long gameTime = level.getGameTime();
        Forced forced = forced(0);

        pressure = Mth.clamp(pressureTarget(profile, dimensionOffset, gameTime) + forced.pressure(),
                profile.pMin, profile.pMax);
        pressureVel = 0f;
        airTemp = baseTemperature(type, level) + forced.temperature();
        dewPoint = Mth.clamp(dewPointTarget(profile, dimensionOffset, gameTime, pressure) + forced.dewPoint(),
                profile.dpMin, profile.dpMax);
        dewPointVel = 0f;
    }

    public static float getRecommendedDewPoint(WeatherTypes type, ServerLevel level) {
        DimensionProfile profile = DimensionProfile.of(level);
        float base = WeatherDefinitions.get(type).recommendedDewPoint;
        float offset = SeasonModifiers.dewPointOffset(level);
        return Mth.clamp(base + offset, profile.dpMin, profile.dpMax);
    }

    // FORECAST

    public AtmosphericSystem forecastState(int inGameMinutes, long currentGameTime, ServerLevel level) {
        return forecastState(inGameMinutes, currentGameTime, level, lastWeather);
    }

    public AtmosphericSystem forecastState(int inGameMinutes, long currentGameTime, ServerLevel level,
                                           WeatherTypes weatherType) {
        DimensionProfile profile = DimensionProfile.of(level);
        long dimensionOffset = getDimensionSeedOffset(level);
        int ahead = inGameMinutes * 1200;
        long futureTime = currentGameTime + ahead;
        long tod = (level.getDayTime() + ahead) % 24000;

        AtmosphericSystem future = new AtmosphericSystem();
        future.mode = mode;
        future.softPMin = softPMin;   future.softPMax = softPMax;
        future.softDPMin = softDPMin; future.softDPMax = softDPMax;
        for (AtmosphericForcing f : activeForcings) {
            AtmosphericForcing later = f.advanced(ahead);
            if (later != null) future.activeForcings.add(later);
        }
        Forced forced = future.forced(0);

        future.pressure = Mth.clamp(pressureTarget(profile, dimensionOffset, futureTime) + forced.pressure(),
                profile.pMin, profile.pMax);
        future.airTemp = baseTemperature(weatherType, level) + forced.temperature();

        float dp = Mth.clamp(dewPointTarget(profile, dimensionOffset, futureTime, future.pressure) + forced.dewPoint(),
                profile.dpMin, profile.dpMax);
        float temp = temperatureOf(profile, future.airTemp, dp, future.pressure, tod);
        future.dewPoint = Mth.clamp(Math.min(dp, temp), profile.dpMin, profile.dpMax);

        future.lastTemperature = temperatureOf(profile, future.airTemp, future.dewPoint, future.pressure, tod);
        future.lastHumidity = Mth.clamp(computeRelativeHumidity(future.lastTemperature, future.dewPoint), 0f, 100f);
        future.lastWindIntensity = lastWindIntensity;
        future.lastWeather = weatherType;
        return future;
    }

    public float forecastTemperatureFromState(AtmosphericSystem state, int min, long t, WeatherTypes wt, ServerLevel level) {
        DimensionProfile profile = DimensionProfile.of(level);
        long tod = (level.getDayTime() + min * 1200L) % 24000;
        float air = baseTemperature(wt, level) + state.forced(0).temperature();
        return temperatureOf(profile, air, state.dewPoint, state.pressure, tod);
    }

    public float forecastHumidityFromState(AtmosphericSystem state, WeatherTypes wt, int min, long t, ServerLevel level) {
        float futureT = forecastTemperatureFromState(state, min, t, wt, level);
        return Mth.clamp(computeRelativeHumidity(futureT, state.dewPoint), 0f, 100f);
    }

    public float getNormalized(ServerLevel level) {
        DimensionProfile profile = DimensionProfile.of(level);
        return (pressure - profile.pMin) / (profile.pMax - profile.pMin);
    }

    public PressureCategory getCategory(ServerLevel level) {
        float normalized = getNormalized(level);
        if (normalized > 0.7f) return PressureCategory.HIGH;
        if (normalized > 0.5f) return PressureCategory.NEUTRAL;
        if (normalized > 0.3f) return PressureCategory.LOW;
        return PressureCategory.STORM;
    }

    public enum PressureCategory { HIGH, NEUTRAL, LOW, STORM }

    public float forecastPressure(int min, long t, ServerLevel level) {
        return forecastState(min, t, level).getPressure();
    }

    public float forecastTemperature(int min, long t, WeatherTypes wt, ServerLevel level) {
        return forecastState(min, t, level, wt).getTemperature();
    }

    public float forecastHumidity(int min, long t, WeatherTypes wt, ServerLevel level) {
        return forecastState(min, t, level, wt).getHumidity();
    }

    public float getPressure()      { return pressure; }
    public float getPressureVel()   { return pressureVel; }
    public float getDewPoint()      { return dewPoint; }
    public float getTemperature()   { return lastTemperature; }
    public float getWeatherTemperature() { return lastWeatherTemperature; }
    public float getHumidity()      { return lastHumidity; }
    public float getWindIntensity() { return lastWindIntensity; }
    public Mode  getMode()          { return mode; }

    public void applyForcing(float pressureOffset, float dewPointOffset, float tempOffset, int durationTicks, AtmosphericForcing.Bias bias) {
        activeForcings.add(new AtmosphericForcing(
                pressureOffset, dewPointOffset, tempOffset, durationTicks, bias));
    }

    public int getActiveForcingCount() { return activeForcings.size(); }

    public void clearForcings() { activeForcings.clear(); }

    private Forced forced(int ticksAhead) {
        float p = 0f, dp = 0f, t = 0f;
        for (AtmosphericForcing f : activeForcings) {
            float strength = f.strengthIn(ticksAhead);
            p  += f.pressureOffset() * strength;
            dp += f.dewPointOffset() * strength;
            t  += f.tempOffset() * strength;
        }
        return new Forced(
                Mth.clamp(p, -MAX_FORCED_PRESSURE, MAX_FORCED_PRESSURE),
                Mth.clamp(dp, -MAX_FORCED_DEW_POINT, MAX_FORCED_DEW_POINT),
                Mth.clamp(t, -MAX_FORCED_TEMP, MAX_FORCED_TEMP));
    }

    public float biasStrength(AtmosphericForcing.Bias bias) {
        float strength = 0f;
        for (AtmosphericForcing f : activeForcings) {
            if (f.bias() == bias) strength = Math.max(strength, f.strength());
        }
        return strength;
    }

    public float biasModifier(WeatherTypes target) {
        if (activeForcings.isEmpty()) return 1f;

        float modifier = 1f;
        for (AtmosphericForcing.Bias bias : AtmosphericForcing.Bias.values()) {
            if (bias == AtmosphericForcing.Bias.NONE) continue;
            float strength = biasStrength(bias);
            if (strength > 0f) modifier *= Mth.lerp(strength, 1f, AtmosphericForcing.biasModifier(target, bias));
        }
        return modifier;
    }

    public AtmosphericForcing.Bias dominantBias() {
        AtmosphericForcing.Bias dominant = AtmosphericForcing.Bias.NONE;
        float strongest = 0f;
        for (AtmosphericForcing f : activeForcings) {
            if (f.bias() != AtmosphericForcing.Bias.NONE && f.strength() > strongest) {
                strongest = f.strength();
                dominant = f.bias();
            }
        }
        return dominant;
    }

    // NBT

    public void save(CompoundTag tag) {
        tag.putFloat("pressure",    pressure);
        tag.putFloat("pressureVel", pressureVel);
        tag.putFloat("dewPoint",    dewPoint);
        tag.putFloat("dewPointVel", dewPointVel);
        if (!Float.isNaN(airTemp)) tag.putFloat("airTemp", airTemp);
        tag.putString("atmosMode",  mode.name());
        tag.putFloat("softPMin",    softPMin);
        tag.putFloat("softPMax",    softPMax);
        tag.putFloat("softDPMin",   softDPMin);
        tag.putFloat("softDPMax",   softDPMax);
        tag.putInt("manualTicks",   manualTicksRemaining);
        ListTag forcings = new ListTag();
        for (AtmosphericForcing f : activeForcings) forcings.add(f.save());
        tag.put("Forcings", forcings);
    }

    public void load(CompoundTag tag) {
        pressure    = tag.contains("pressure")    ? tag.getFloat("pressure")    : P_NOMINAL;
        pressureVel = tag.contains("pressureVel") ? tag.getFloat("pressureVel") : 0f;
        dewPoint    = tag.contains("dewPoint")    ? tag.getFloat("dewPoint")    : 8f;
        dewPointVel = tag.contains("dewPointVel") ? tag.getFloat("dewPointVel") : 0f;
        airTemp     = tag.contains("airTemp")     ? tag.getFloat("airTemp")     : Float.NaN;
        softPMin  = tag.contains("softPMin")  ? tag.getFloat("softPMin")  : NO_BOUND_MIN;
        softPMax  = tag.contains("softPMax")  ? tag.getFloat("softPMax")  : NO_BOUND_MAX;
        softDPMin = tag.contains("softDPMin") ? tag.getFloat("softDPMin") : NO_BOUND_MIN;
        softDPMax = tag.contains("softDPMax") ? tag.getFloat("softDPMax") : NO_BOUND_MAX;
        manualTicksRemaining = tag.contains("manualTicks") ? tag.getInt("manualTicks") : 0;
        mode = tag.contains("atmosMode")
                ? Mode.valueOf(tag.getString("atmosMode"))
                : Mode.PROCEDURAL;
        activeForcings.clear();
        ListTag forcingList = tag.getList("Forcings", Tag.TAG_COMPOUND);
        for (Tag t : forcingList) activeForcings.add(AtmosphericForcing.load((CompoundTag) t));
    }
}
