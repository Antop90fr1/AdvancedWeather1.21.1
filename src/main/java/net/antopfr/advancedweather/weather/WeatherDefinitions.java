package net.antopfr.advancedweather.weather;

import net.minecraft.world.phys.Vec3;

import java.util.EnumMap;
import java.util.Map;

public class WeatherDefinitions {

    private static final Map<WeatherTypes, WeatherDefinition> MAP =
            new EnumMap<>(WeatherTypes.class);

    private static WeatherDefinition DEFAULT;

    static void register(WeatherTypes type, WeatherDefinition def) {
        MAP.put(type, def);
    }

    public static WeatherDefinition get(WeatherTypes type) {
        WeatherDefinition d = MAP.get(type);
        return d != null ? d : DEFAULT;
    }

    static {
        // OVERWORLD
        DEFAULT = WeatherDefinition.of(WeatherTypes.CLEAR)
                .temperature(16f).wind(0.05f)
                .pressure(1035f).dewPoint(5f)
                .duration(18000, 36000)
                .clouds(new Vec3(1.00, 1.00, 1.00), 0.160f)
                .cloudAltitude(192f).coverage(0.04f, 0.04f).cloudShape(CloudShape.FAIR_CUMULUS)
                .effects(WeatherEffects.WIND_LINES, WeatherEffects.RAINBOWS)
                .build();

        WeatherDefinition.of(WeatherTypes.SUNNY)
                .temperature(14f).wind(0.05f)
                .pressure(1035f).dewPoint(5f)
                .duration(12000, 24000)
                .clouds(new Vec3(1.00, 1.00, 1.00), 0.200f)
                .cloudAltitude(184f).coverage(0.12f, 0.08f).cloudShape(CloudShape.FAIR_CUMULUS)
                .effects(WeatherEffects.WIND_LINES, WeatherEffects.RAINBOWS)
                .build();

        WeatherDefinition.of(WeatherTypes.CLOUDY)
                .temperature(12f).wind(0.05f)
                .pressure(1015f).dewPoint(10f)
                .duration(6000, 24000)
                .clouds(new Vec3(0.88, 0.89, 0.91), 0.300f)
                .cloudAltitude(160f).coverage(0.40f, 0.12f).cloudShape(CloudShape.CUMULUS)
                .skyDesaturation(0.25f)
                .effects(WeatherEffects.WIND_LINES, WeatherEffects.RAINBOWS)
                .build();

        WeatherDefinition.of(WeatherTypes.OVERCAST)
                .temperature(9f).wind(0.15f)
                .pressure(1005f).dewPoint(12f)
                .duration(6000, 18000)
                .clouds(new Vec3(0.64, 0.65, 0.68), 0.400f)
                .cloudAltitude(144f).coverage(0.88f, 0.08f).cloudShape(CloudShape.STRATUS)
                .skyDesaturation(0.85f)
                .hidesSun(true)
                .effects(WeatherEffects.WIND_LINES)
                .build();

        WeatherDefinition.of(WeatherTypes.MIST)
                .temperature(10f).wind(0.05f)
                .pressure(1015f).dewPoint(10f)
                .duration(3000, 9000)
                .clouds(new Vec3(0.86, 0.86, 0.88), 0.260f)
                .cloudAltitude(160f).coverage(0.35f, 0.12f).cloudShape(CloudShape.STRATUS)
                .skyDesaturation(0.25f)
                .effects(WeatherEffects.WIND_LINES, WeatherEffects.GROUND_FOG)
                .build();

        WeatherDefinition.of(WeatherTypes.FOG)
                .temperature(8f).wind(0.05f)
                .pressure(1005f).dewPoint(14f)
                .duration(9000, 18000)
                .clouds(new Vec3(0.80, 0.80, 0.82), 0.300f)
                .cloudAltitude(160f).coverage(0f, 0f).cloudShape(CloudShape.STRATUS)
                .skyDesaturation(0.60f)
                .hidesSun(true)
                .fog(5f, 25f)
                .effects(WeatherEffects.WIND_LINES, WeatherEffects.GROUND_FOG)
                .build();

        WeatherDefinition.of(WeatherTypes.DENSE_FOG)
                .temperature(7f).wind(0.05f)
                .pressure(1005f).dewPoint(14f)
                .duration(3000, 9000)
                .clouds(new Vec3(0.80, 0.80, 0.82), 0.300f)
                .cloudAltitude(160f).coverage(0f, 0f).cloudShape(CloudShape.STRATUS)
                .skyDesaturation(0.60f)
                .hidesSun(true)
                .fog(1f, 8f)
                .effects(WeatherEffects.WIND_LINES, WeatherEffects.GROUND_FOG)
                .build();

        WeatherDefinition.of(WeatherTypes.DRIZZLE)
                .temperature(9f).wind(0.15f)
                .pressure(995f).dewPoint(13f)
                .duration(3000, 12000)
                .clouds(new Vec3(0.76, 0.77, 0.80), 0.514f)
                .cloudAltitude(160f).coverage(0.70f, 0.08f).cloudShape(CloudShape.STRATUS)
                .skyDesaturation(0.55f)
                .rainIntensity(0.2f)
                .hidesSun(false)
                .effects(WeatherEffects.WIND_LINES, WeatherEffects.RAINBOWS)
                .build();

        WeatherDefinition.of(WeatherTypes.LIGHT_RAIN)
                .temperature(8f).wind(0.15f)
                .pressure(995f).dewPoint(14f)
                .duration(6000, 18000)
                .clouds(new Vec3(0.70, 0.71, 0.74), 0.514f)
                .cloudAltitude(144f).coverage(0.82f, 0.08f).cloudShape(CloudShape.NIMBOSTRATUS)
                .skyDesaturation(0.55f)
                .rainIntensity(0.4f)
                .hidesSun(true)
                .effects(WeatherEffects.WIND_LINES)
                .build();

        WeatherDefinition.of(WeatherTypes.HEAVY_RAIN)
                .temperature(6f).wind(0.45f)
                .pressure(980f).dewPoint(18f)
                .duration(4000, 18000)
                .clouds(new Vec3(0.55, 0.57, 0.61), 0.629f)
                .cloudAltitude(128f).coverage(0.94f, 0.08f).cloudShape(CloudShape.NIMBOSTRATUS)
                .skyDesaturation(0.85f)
                .rainIntensity(1.5f)
                .hidesSun(true)
                .effects(WeatherEffects.WIND_LINES)
                .build();

        WeatherDefinition.of(WeatherTypes.FREEZING_RAIN)
                .temperature(0f).wind(0.15f)
                .pressure(980f).dewPoint(-1f)
                .duration(2000, 6000)
                .clouds(new Vec3(0.70, 0.72, 0.76), 0.400f)
                .cloudAltitude(160f).coverage(0.90f, 0.08f).cloudShape(CloudShape.NIMBOSTRATUS)
                .skyDesaturation(0.70f)
                .rainIntensity(0.8f)
                .hidesSun(true)
                .fog(10f, 60f)
                .effects(WeatherEffects.WIND_LINES)
                .build();

        WeatherDefinition.of(WeatherTypes.THUNDERSTORM)
                .temperature(13f).wind(0.75f)
                .pressure(962f).dewPoint(20f)
                .duration(2000, 6000)
                .clouds(new Vec3(0.42, 0.44, 0.50), 0.629f)
                .cloudAltitude(128f).coverage(0.96f, 0.04f).cloudShape(CloudShape.CUMULONIMBUS)
                .skyDesaturation(0.90f)
                .rainIntensity(1.7f)
                .hidesSun(true)
                .fog(15f, 80f)
                .effects(WeatherEffects.WIND_LINES)
                .build();

        WeatherDefinition.of(WeatherTypes.SNOW)
                .temperature(-3f).wind(0.15f)
                .pressure(995f).dewPoint(-5f)
                .duration(9000, 18000)
                .clouds(new Vec3(0.82, 0.84, 0.88), 0.514f)
                .cloudAltitude(144f).coverage(0.85f, 0.08f).cloudShape(CloudShape.NIMBOSTRATUS)
                .skyDesaturation(0.70f)
                .rainIntensity(0.5f)
                .hidesSun(true)
                .effects(WeatherEffects.WIND_LINES)
                .build();

        WeatherDefinition.of(WeatherTypes.BLIZZARD)
                .temperature(-6f).wind(0.75f)
                .pressure(980f).dewPoint(-8f)
                .duration(3000, 9000)
                .clouds(new Vec3(0.80, 0.83, 0.88), 0.629f)
                .cloudAltitude(128f).coverage(0.97f, 0.04f).cloudShape(CloudShape.NIMBOSTRATUS)
                .skyDesaturation(0.90f)
                .rainIntensity(1.0f)
                .hidesSun(true)
                .fog(5f, 25f)
                .effects(WeatherEffects.WIND_LINES, WeatherEffects.BLIZZARD_PARTICLES)
                .build();

        WeatherDefinition.of(WeatherTypes.HAIL)
                .temperature(12f).wind(0.45f)
                .pressure(962f).dewPoint(15f)
                .duration(1000, 3000)
                .clouds(new Vec3(0.62, 0.64, 0.68), 0.400f)
                .cloudAltitude(144f).coverage(0.92f, 0.08f).cloudShape(CloudShape.CUMULONIMBUS)
                .skyDesaturation(0.70f)
                .rainIntensity(0.7f)
                .hidesSun(false)
                .fog(15f, 45f)
                .effects(WeatherEffects.WIND_LINES)
                .build();

        WeatherDefinition.of(WeatherTypes.WINDY)
                .temperature(13f).wind(0.65f)
                .pressure(1035f).dewPoint(4f)
                .duration(6000, 24000)
                .clouds(new Vec3(0.95, 0.96, 0.97), 0.300f)
                .cloudAltitude(184f).coverage(0.26f, 0.12f).cloudShape(CloudShape.FAIR_CUMULUS)
                .effects(WeatherEffects.WIND_LINES)
                .build();

        WeatherDefinition.of(WeatherTypes.SANDSTORM)
                .temperature(30f).wind(0.65f)
                .pressure(962f).dewPoint(-8f)
                .duration(3000, 12000)
                .clouds(new Vec3(0.82, 0.70, 0.50), 0.260f)
                .cloudAltitude(160f).coverage(0.45f, 0.08f).cloudShape(CloudShape.CUMULUS)
                .skyDesaturation(0.60f)
                .fog(5f, 22f)
                .effects(WeatherEffects.WIND_LINES, WeatherEffects.SAND_PARTICLES,
                        WeatherEffects.TUMBLEWEEDS)
                .build();

        // NETHER
        WeatherDefinition.of(WeatherTypes.NETHER_CLEAR)
                .temperature(65f).wind(0.10f)
                .pressure(1150f).dewPoint(-25f)
                .duration(18000, 36000)
                .effects(WeatherEffects.NETHER_WIND_LINES)
                .build();

        WeatherDefinition.of(WeatherTypes.ASH_STORM)
                .temperature(85f).wind(0.60f)
                .pressure(1130f).dewPoint(-25f)
                .duration(6000, 18000)
                .fog(5f, 22f)
                .effects(WeatherEffects.NETHER_WIND_LINES, WeatherEffects.ASH_PARTICLES)
                .build();

        WeatherDefinition.of(WeatherTypes.BRIMSTONE_STORM)
                .temperature(75f).wind(0.80f)
                .pressure(1160f).dewPoint(-25f)
                .duration(3000, 9000)
                .fog(4f, 24f)
                .effects(WeatherEffects.NETHER_WIND_LINES, WeatherEffects.BRIMSTONE_PARTICLES)
                .build();

        WeatherDefinition.of(WeatherTypes.LAVA_RAIN)
                .temperature(100f).wind(0.40f)
                .pressure(1140f).dewPoint(-25f)
                .duration(2000, 6000)
                .effects(WeatherEffects.NETHER_WIND_LINES)
                .build();

        WeatherDefinition.of(WeatherTypes.NETHERSTORM)
                .temperature(80f).wind(0.95f)
                .pressure(1170f).dewPoint(-25f)
                .duration(2000, 5000)
                .fog(20f, 50f)
                .effects(WeatherEffects.NETHER_WIND_LINES, WeatherEffects.FIRE_PARTICLES)
                .build();

        WeatherDefinition.of(WeatherTypes.HELLFIRE)
                .temperature(115f).wind(0.15f)
                .pressure(1180f).dewPoint(-25f)
                .duration(4000, 12000)
                .fog(12f, 64f)
                .effects(WeatherEffects.NETHER_WIND_LINES, WeatherEffects.HEAT_SHIMMER)
                .build();

        // END
        WeatherDefinition.of(WeatherTypes.END_CLEAR)
                .temperature(-34f).wind(0.10f)
                .pressure(900f).dewPoint(-28f)
                .duration(18000, 36000)
                .effects(WeatherEffects.END_WIND_LINES)
                .build();

        WeatherDefinition.of(WeatherTypes.VOID_STORM)
                .temperature(-20f).wind(0.70f)
                .pressure(890f).dewPoint(-28f)
                .duration(4000, 12000)
                .effects(WeatherEffects.END_WIND_LINES, WeatherEffects.VOID_PARTICLES)
                .build();

        WeatherDefinition.of(WeatherTypes.END_MIST)
                .temperature(-15f).wind(0.20f)
                .pressure(920f).dewPoint(-28f)
                .duration(6000, 18000)
                .effects(WeatherEffects.END_WIND_LINES, WeatherEffects.END_GROUND_FOG)
                .build();

        WeatherDefinition.of(WeatherTypes.CHORUS_GALE)
                .temperature(-18f).wind(0.85f)
                .pressure(880f).dewPoint(-28f)
                .duration(3000, 9000)
                .effects(WeatherEffects.END_WIND_LINES, WeatherEffects.CHORUS_PLANTS, WeatherEffects.CHORUS_PETALS)
                .build();

        WeatherDefinition.of(WeatherTypes.ENDERSTORM)
                .temperature(-25f).wind(0.70f)
                .pressure(870f).dewPoint(-28f)
                .duration(2000, 6000)
                .effects(WeatherEffects.END_WIND_LINES, WeatherEffects.ENDER_PARTICLES,
                        WeatherEffects.CHROMATIC_EFFECT)
                .build();
    }
}
