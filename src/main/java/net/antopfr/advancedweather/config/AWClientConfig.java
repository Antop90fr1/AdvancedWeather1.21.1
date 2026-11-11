package net.antopfr.advancedweather.config;

import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import net.antopfr.advancedweather.client.gui.hud.WeatherHudOverlay;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLPaths;
import java.nio.file.Path;

public class AWClientConfig {
    private static final Path PATH = FMLPaths.CONFIGDIR.get().resolve("advancedweather.json");

    private static final ConfigClassHandler<AWClientConfig> HANDLER = ConfigClassHandler.createBuilder(AWClientConfig.class)
            .id(ResourceLocation.fromNamespaceAndPath("advancedweather", "config"))
            .serializer(config -> GsonConfigSerializerBuilder.create(config)
                    .setPath(PATH)
                    .build())
            .build();

    // weather hud
    @SerialEntry public boolean enableWeatherHud = true;
    @SerialEntry public boolean weatherHudShowTime = true;
    @SerialEntry public WeatherHudOverlay.HudPosition weatherHudPosition = WeatherHudOverlay.HudPosition.TOP_RIGHT;

    // units
    @SerialEntry public boolean useFahrenheit = false;
    @SerialEntry public boolean useMph = false;

    // clouds
    @SerialEntry public int cloudRenderDistance = 384;

    @SerialEntry public boolean frozenGround = true;
    @SerialEntry public boolean snowCover = true;
    @SerialEntry public boolean snowCoverOnSnow = false;
    @SerialEntry public boolean sandCover = true;

    @SerialEntry public boolean distantStorms = true;

    @SerialEntry public boolean foliageSway = true;
    @SerialEntry public double foliageSwayStrength = 1.0;

    @SerialEntry public int windLineMax = 80;
    @SerialEntry public double windLineLengthMin = 1.5;
    @SerialEntry public double windLineLengthMax = 4.5;
    @SerialEntry public double windLineSpawnDistMin = 15.0;
    @SerialEntry public double windLineSpawnDistMax = 25.0;
    @SerialEntry public double windLineLateralSpread = 35.0;
    @SerialEntry public double windLineHeightSpread = 9.0;

    // wind on particles
    @SerialEntry public boolean windPushesParticles = true;
    @SerialEntry public double windParticleStrength = 1.0;

    @SerialEntry public boolean particleRainIntegration = true;
    @SerialEntry public boolean atmosphericsIntegration = true;

    // blizzard
    @SerialEntry public int blizzardCount = 48;
    @SerialEntry public int blizzardTickInterval = 2;
    @SerialEntry public double blizzardSpawnDist = 8.0;
    @SerialEntry public double blizzardSpread = 16.0;
    @SerialEntry public double blizzardHeightSpread = 12.0;
    @SerialEntry public double blizzardWindSpeed = 0.5;
    @SerialEntry public double blizzardSizeMin = 0.14;
    @SerialEntry public double blizzardSizeMax = 0.25;

    // sand
    @SerialEntry public int sandCount = 48;
    @SerialEntry public int sandTickInterval = 2;
    @SerialEntry public double sandSpawnDist = 8.0;
    @SerialEntry public double sandSpread = 16.0;
    @SerialEntry public double sandHeightSpread = 8.0;
    @SerialEntry public double sandWindSpeed = 0.8;
    @SerialEntry public double sandSizeMin = 0.22;
    @SerialEntry public double sandSizeMax = 0.27;

    // brimstone
    @SerialEntry public int brimstoneCount = 48;
    @SerialEntry public int brimstoneTickInterval = 2;
    @SerialEntry public double brimstoneSpawnDist = 8.0;
    @SerialEntry public double brimstoneSpread = 16.0;
    @SerialEntry public double brimstoneHeightSpread = 8.0;
    @SerialEntry public double brimstoneWindSpeed = 0.8;
    @SerialEntry public double brimstoneSizeMin = 0.22;
    @SerialEntry public double brimstoneSizeMax = 0.27;

    // ground fog
    @SerialEntry public double groundFogAlphaMin = 0.1;
    @SerialEntry public double groundFogAlphaMax = 0.35;
    @SerialEntry public double groundFogSizeMin = 0.6;
    @SerialEntry public double groundFogSizeMax = 1.7;
    @SerialEntry public double groundFogWidthScale = 3.5;
    @SerialEntry public int groundFogLifetimeMin = 300;
    @SerialEntry public int groundFogLifetimeMax = 500;

    public static AWClientConfig get() {
        return HANDLER.instance();
    }
    public void save() {
        HANDLER.save();
    }
    public static void init() {
        HANDLER.load();
    }
}