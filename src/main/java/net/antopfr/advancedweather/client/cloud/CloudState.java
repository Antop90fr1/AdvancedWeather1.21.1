package net.antopfr.advancedweather.client.cloud;

import net.antopfr.advancedweather.client.state.ClientAtmosphereState;
import net.antopfr.advancedweather.client.state.ClientWeatherState;
import net.antopfr.advancedweather.weather.BiomeFogData;
import net.antopfr.advancedweather.weather.CloudShape;
import net.antopfr.advancedweather.weather.WeatherDefinition;
import net.antopfr.advancedweather.weather.WeatherDefinitions;
import net.antopfr.advancedweather.weather.WeatherTypes;
import net.antopfr.advancedweather.weather.wind.WindField;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class CloudState {

    private CloudState() {}

    private static final float RATE = 0.004f;
    private static final float VANISH_RATE = 0.02f;
    private static final float BIOME_INFLUENCE = 0.35f;

    private static ClientLevel trackedLevel;

    private static float coverage;
    private static float altitude = 160f;
    private static float opacity = 0.85f;
    private static float minThickness = CloudShape.CUMULUS.minThickness;
    private static float maxThickness = CloudShape.CUMULUS.maxThickness;
    private static float puff = CloudShape.CUMULUS.puff;
    private static float heightCurve = CloudShape.CUMULUS.heightCurve;
    private static float tintR = 1f, tintG = 1f, tintB = 1f;

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;

        boolean snap = level != trackedLevel;
        trackedLevel = level;

        WeatherTypes weather = ClientWeatherState.getCurrentWeather();
        WeatherDefinition def = WeatherDefinitions.get(weather);

        float humidity = Mth.clamp((ClientAtmosphereState.getLocalHumidity() - 60f) / 40f, -1f, 1f);
        float targetCoverage = Mth.clamp(def.cloudCoverage + humidity * def.coverageMargin, 0f, 1f);
        float targetOpacity = Mth.lerp(def.cloudOpacity, 0.55f, 0.82f);
        CloudShape shape = def.cloudShape;
        Vec3 tint = targetTint(mc, level, weather, def);

        float rate = snap ? 1f : RATE;
        coverage = Mth.lerp(snap || targetCoverage > 0f ? rate : VANISH_RATE, coverage, targetCoverage);
        altitude = Mth.lerp(rate, altitude, def.cloudAltitude);
        opacity = Mth.lerp(rate, opacity, targetOpacity);
        minThickness = Mth.lerp(rate, minThickness, shape.minThickness);
        maxThickness = Mth.lerp(rate, maxThickness, shape.maxThickness);
        puff = Mth.lerp(rate, puff, shape.puff);
        heightCurve = Mth.lerp(rate, heightCurve, shape.heightCurve);
        tintR = Mth.lerp(rate, tintR, (float) tint.x);
        tintG = Mth.lerp(rate, tintG, (float) tint.y);
        tintB = Mth.lerp(rate, tintB, (float) tint.z);
    }

    private static Vec3 targetTint(Minecraft mc, ClientLevel level, WeatherTypes weather, WeatherDefinition def) {
        Vec3 tint = def.cloudTint;
        if (!weather.hasFog()) return tint;

        BlockPos pos = mc.player != null ? mc.player.blockPosition() : BlockPos.ZERO;
        ResourceLocation biome = level.getBiome(pos).unwrapKey().map(ResourceKey::location).orElse(null);
        int fog = BiomeFogData.getColor(level, weather, biome);
        return tint.lerp(normalizedHue(fog), BIOME_INFLUENCE);
    }

    private static Vec3 normalizedHue(int rgb) {
        double r = ((rgb >> 16) & 0xFF) / 255.0;
        double g = ((rgb >> 8) & 0xFF) / 255.0;
        double b = (rgb & 0xFF) / 255.0;
        double lum = Math.max(0.001, (r + g + b) / 3.0);
        return new Vec3(r / lum, g / lum, b / lum);
    }

    public static boolean visible(Level level) {
        return level.dimension() == Level.OVERWORLD && coverage > 0.003f;
    }

    public static float threshold() { return 1f - coverage; }

    public static float coverage()     { return coverage; }
    public static float altitude()     { return altitude; }
    public static float opacity()      { return opacity; }
    public static float minThickness() { return minThickness; }
    public static float maxThickness() { return maxThickness; }
    public static float puff()         { return puff; }
    public static float heightCurve()  { return heightCurve; }
    public static float tintR()        { return tintR; }
    public static float tintG()        { return tintG; }
    public static float tintB()        { return tintB; }

    public static double driftX(float partialTick) { return WindField.clientState().upperX(partialTick); }
    public static double driftZ(float partialTick) { return WindField.clientState().upperZ(partialTick); }
}
