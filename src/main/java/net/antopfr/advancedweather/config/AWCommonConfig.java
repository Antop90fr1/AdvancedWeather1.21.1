package net.antopfr.advancedweather.config;

import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLPaths;

import java.nio.file.Path;

public class AWCommonConfig {

    private static final Path PATH = FMLPaths.CONFIGDIR.get().resolve("advancedweather-common.json");

    private static final ConfigClassHandler<AWCommonConfig> HANDLER = ConfigClassHandler.createBuilder(AWCommonConfig.class)
            .id(ResourceLocation.fromNamespaceAndPath("advancedweather", "config_common"))
            .serializer(config -> GsonConfigSerializerBuilder.create(config)
                    .setPath(PATH)
                    .build())
            .build();

    // WEATHER INSTRUMENTS
    @SerialEntry public boolean stationRequiresSensors = true;
    @SerialEntry public double sensorLinkMaxDistance = 32.0;

    // WEATHER REPORTS
    @SerialEntry public boolean enableWeatherReports = true;          // impression sur la station
    @SerialEntry public int archiveMaxRecords = 20000;                // capacité mémoire
    @SerialEntry public int archiveMinRecordsForForecast = 15;        // en-dessous : aucune prévision
    @SerialEntry public int archiveConfidenceFloorRecords = 15;       // début de la courbe
    @SerialEntry public int archiveConfidenceSaturationRecords = 200; // pleine confiance
    @SerialEntry public long archiveForecastHorizonTicks = 1200;      // horizon de projection (1200 ticks)
    @SerialEntry public long archiveAgingDataTicks = 12000;           // seuil [aging data]
    @SerialEntry public long archiveOutdatedDataTicks = 24000;        // seuil [outdated]
    @SerialEntry public boolean archiveRejectOlderReports = true;     // règle d'antériorité
    @SerialEntry public int reportPrintingTicks = 30;
    @SerialEntry public int almanacMaxRecords = 32;

    // REAL WEATHER
    @SerialEntry public boolean realWeatherEnabled = false;
    @SerialEntry public double latitude = 48.8566;
    @SerialEntry public double longitude = 2.3522;
    @SerialEntry public int syncIntervalMinutes = 4;
    @SerialEntry public boolean realWeatherOverridesCommands = false;

    // WEATHER TYPES - OVERWORLD
    @SerialEntry public boolean enableFog = true;
    @SerialEntry public boolean enableDenseFog = true;
    @SerialEntry public boolean enableMist = true;
    @SerialEntry public boolean enableSandstorm = true;
    @SerialEntry public boolean enableBlizzard = true;
    @SerialEntry public boolean enableFreezingRain = true;
    @SerialEntry public boolean enableHail = true;
    @SerialEntry public boolean enableThunderstorm = true;

    // WEATHER TYPES - NETHER
    @SerialEntry public boolean enableNetherWeather = true;
    @SerialEntry public boolean enableAshStorm = true;
    @SerialEntry public boolean enableBrimstoneStorm = true;
    @SerialEntry public boolean enableLavaRain = true;
    @SerialEntry public boolean enableNetherstorm = true;
    @SerialEntry public boolean enableHellfire = true;

    // WEATHER TYPES - END
    @SerialEntry public boolean enableEndWeather = true;
    @SerialEntry public boolean enableVoidStorm = true;
    @SerialEntry public boolean enableEndMist = true;
    @SerialEntry public boolean enableChorusGale = true;
    @SerialEntry public boolean enableEnderstorm = true;

    // GAMEPLAY
    @SerialEntry public double snowCoverMinutes = 4.0;
    @SerialEntry public boolean hailDamageCrops = true;
    @SerialEntry public boolean hailDamageEntities = true;
    @SerialEntry public boolean lavaRainBurnsEntities = true;
    @SerialEntry public boolean voidStormUpdrafts = true;
    @SerialEntry public boolean hailBreakGlass = true;
    @SerialEntry public boolean heavyRainSlowCrops = true;
    @SerialEntry public boolean sandstormAffectsPlayers = true;   // slowness + hunger when exposed in a sandy biome
    @SerialEntry public boolean rainbowBringsLuck = true;
    @SerialEntry public boolean fogHidesPlayers = true;
    @SerialEntry public boolean denseFogSpawnsMonsters = true;

    // AGRICULTURE
    @SerialEntry public boolean rainBoostsCrops = true;
    @SerialEntry public boolean sunBoostsCrops = true;
    @SerialEntry public boolean frostKillsCrops = true;
    @SerialEntry public boolean droughtDriesFarmland = true;

    // WIND
    @SerialEntry public boolean windSpreadsFire = true;
    @SerialEntry public boolean windAffectsContraptions = true;
    @SerialEntry public double contraptionWindStrength = 1.0;

    @SerialEntry public boolean coldSweatIntegration = true;
    @SerialEntry public double coldSweatWeatherStrength = 0.75;
    @SerialEntry public boolean coldSweatWindChill = true;
    @SerialEntry public boolean coldSweatReplaceStormModifier = true;

    // CREATE
    @SerialEntry public boolean enableCreateWindmillCompat = true;
    @SerialEntry public double windmillWindThreshold = 0.4;
    @SerialEntry public boolean windmillDirectionAffectsOutput = true;
    @SerialEntry public double windmillSpeedScaleFactor = 0.025;


    public static AWCommonConfig get() {
        return HANDLER.instance();
    }
    public void save() {
        HANDLER.save();
    }
    public static void init() {
        HANDLER.load();
    }
}