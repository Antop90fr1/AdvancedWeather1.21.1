package net.antopfr.advancedweather.weather;

import net.antopfr.advancedweather.util.AWRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.EnumMap;
import java.util.Map;

public class BiomeFogData {

    private static final Map<WeatherTypes, Integer> DEFAULTS = new EnumMap<>(WeatherTypes.class);
    static {
        DEFAULTS.put(WeatherTypes.FOG,             0xBFBFBF);
        DEFAULTS.put(WeatherTypes.DENSE_FOG,       0xA6A6AD);
        DEFAULTS.put(WeatherTypes.BLIZZARD,        0xD4DEE6);
        DEFAULTS.put(WeatherTypes.THUNDERSTORM,    0x596173);
        DEFAULTS.put(WeatherTypes.SANDSTORM,       0xD9B05C);
        DEFAULTS.put(WeatherTypes.HAIL,            0xCECFAB);
        DEFAULTS.put(WeatherTypes.MIST,            0xC8CDD0);
        DEFAULTS.put(WeatherTypes.ASH_STORM,       0x54545C);
        DEFAULTS.put(WeatherTypes.BRIMSTONE_STORM, 0x884422);
        DEFAULTS.put(WeatherTypes.LAVA_RAIN,       0xAA4400);
        DEFAULTS.put(WeatherTypes.NETHERSTORM,     0x662200);
        DEFAULTS.put(WeatherTypes.HELLFIRE,        0xCC3300);
        DEFAULTS.put(WeatherTypes.VOID_STORM,      0x05000A);
        DEFAULTS.put(WeatherTypes.END_MIST,        0x4D225E);
        DEFAULTS.put(WeatherTypes.CHORUS_GALE,     0x3A0B4E);
        DEFAULTS.put(WeatherTypes.ENDERSTORM,      0x16002A);
    }

    public static int getColor(Level level, WeatherTypes type, ResourceLocation biome) {
        if (biome != null) {
            var reg = level.registryAccess().registry(AWRegistries.BIOME_FOG).orElse(null);
            if (reg != null) {
                BiomeFog fog = reg.get(biome);
                if (fog != null) {
                    Integer c = fog.get(type);
                    if (c != null) return c;
                }
            }
        }
        return DEFAULTS.getOrDefault(type, 0xBFBFBF);
    }
}
