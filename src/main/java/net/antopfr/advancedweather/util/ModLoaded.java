package net.antopfr.advancedweather.util;

import net.neoforged.fml.ModList;

public class ModLoaded {
    public static boolean CREATE = modLoaded("create");
    public static boolean SERENESEASONS = modLoaded("sereneseasons");
    public static boolean SABLE = modLoaded("sable");
    public static boolean COLDSWEAT = modLoaded("cold_sweat");
    public static boolean PARTICLERAIN = modLoaded("particlerain");
    public static boolean ATMOSPHERICS = modLoaded("atmospherics");
    public static boolean ECLIPTICSEASONS = modLoaded("eclipticseasons");
    public static boolean IRIS = modLoaded("iris");

    public static boolean isCreateLoaded() {
        return CREATE;
    }
    public static boolean isSereneSeasonsLoaded() {
        return SERENESEASONS;
    }
    public static boolean isSableLoaded() {
        return SABLE;
    }
    public static boolean isColdSweatLoaded() {
        return COLDSWEAT;
    }
    public static boolean isParticleRainLoaded() {
        return PARTICLERAIN;
    }
    public static boolean isAtmosphericsLoaded() {
        return ATMOSPHERICS;
    }
    public static boolean isEclipticSeasonsLoaded() {
        return ECLIPTICSEASONS;
    }
    public static boolean isIrisLoaded() {
        return IRIS;
    }

    private static boolean modLoaded(String mod) {
        return (ModList.get().isLoaded(mod));
    }

    public static void logAllCompat() {
        if (ModLoaded.isCreateLoaded()) {
            AWLogger.AW.info("[AdvancedWeather] Create detected, compatibility enabled");
        }
        if (ModLoaded.isSereneSeasonsLoaded()) {
            AWLogger.AW.info("[AdvancedWeather] Serene Seasons detected, compatibility enabled");
        }
        if (ModLoaded.isSableLoaded()) {
            AWLogger.AW.info("[AdvancedWeather] Sable detected, compatibility enabled");
        }
        if (ModLoaded.isColdSweatLoaded()) {
            AWLogger.AW.info("[AdvancedWeather] Cold Sweat detected, compatibility enabled");
        }
        if (ModLoaded.isParticleRainLoaded()) {
            AWLogger.AW.info("[AdvancedWeather] Particle Rain detected, compatibility enabled");
        }
        if (ModLoaded.isAtmosphericsLoaded()) {
            AWLogger.AW.info("[AdvancedWeather] ATMOSPHERICS detected, compatibility enabled");
        }
        if (ModLoaded.isEclipticSeasonsLoaded()) {
            AWLogger.AW.info("[AdvancedWeather] Ecliptic Seasons detected, compatibility enabled");
        }
        if (ModLoaded.isIrisLoaded()) {
            AWLogger.AW.info("[AdvancedWeather] Iris detected, our clouds give way to shader packs");
        }
    }
}
