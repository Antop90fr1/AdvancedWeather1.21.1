package net.antopfr.advancedweather.config;

import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.OptionGroup;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.*;
import net.antopfr.advancedweather.client.gui.hud.WeatherHudOverlay;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.awt.*;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class AWConfigScreen {
    public static Screen create(Screen parent) {
        return new AWConfigSelectScreen(parent);
    }

    // CLIENT
    public static Screen createClient(Screen parent) {
        AWClientConfig config = AWClientConfig.get();

        YetAnotherConfigLib.Builder builder = YetAnotherConfigLib.createBuilder()
                .title(Component.translatable("advancedweather.config.title.client"))
                .save(config::save);

        // WEATHER HUD
        ConfigCategory weatherHud = ConfigCategory.createBuilder()
                .name(Component.translatable("advancedweather.config.category.weather_hud"))
                .option(Option.<Boolean>createBuilder()
                        .name(Component.translatable("advancedweather.config.enable_weather_hud"))
                        .description(OptionDescription.of(Component.translatable("advancedweather.config.enable_weather_hud.desc")))
                        .binding(true, () -> config.enableWeatherHud, v -> config.enableWeatherHud = v)
                        .controller(BooleanControllerBuilder::create)
                        .build())
                .option(Option.<Boolean>createBuilder()
                        .name(Component.translatable("advancedweather.config.show_time"))
                        .description(OptionDescription.of(Component.translatable("advancedweather.config.show_time.desc")))
                        .binding(true, () -> config.weatherHudShowTime, v -> config.weatherHudShowTime = v)
                        .controller(BooleanControllerBuilder::create)
                        .build())
                .option(Option.<WeatherHudOverlay.HudPosition>createBuilder()
                        .name(Component.translatable("advancedweather.config.hud_position"))
                        .binding(WeatherHudOverlay.HudPosition.TOP_RIGHT, () -> config.weatherHudPosition, v -> config.weatherHudPosition = v)
                        .controller(opt -> EnumControllerBuilder.create(opt)
                                .enumClass(WeatherHudOverlay.HudPosition.class)
                                .formatValue(position -> Component.translatable("advancedweather.config.hud_position."
                                        + position.name().toLowerCase(Locale.ROOT))))
                        .build())
                .build();

        // UNITS
        ConfigCategory units = ConfigCategory.createBuilder()
                .name(Component.translatable("advancedweather.config.category.units"))
                .option(Option.<Boolean>createBuilder()
                        .name(Component.translatable("advancedweather.config.use_fahrenheit_unit"))
                        .description(OptionDescription.of(Component.translatable("advancedweather.config.use_fahrenheit_unit.desc")))
                        .binding(true, () -> config.useFahrenheit, v -> config.useFahrenheit = v)
                        .controller(BooleanControllerBuilder::create)
                        .build())
                .option(Option.<Boolean>createBuilder()
                        .name(Component.translatable("advancedweather.config.use_miles_per_hour_unit"))
                        .description(OptionDescription.of(Component.translatable("advancedweather.config.use_miles_per_hour_unit.desc")))
                        .binding(true, () -> config.useMph, v -> config.useMph = v)
                        .controller(BooleanControllerBuilder::create)
                        .build())
                .build();

        // CLOUDS
        ConfigCategory clouds = ConfigCategory.createBuilder()
                .name(Component.translatable("advancedweather.config.category.clouds"))
                .option(Option.<Integer>createBuilder()
                        .name(Component.translatable("advancedweather.config.cloud_render_distance"))
                        .binding(384, () -> config.cloudRenderDistance, v -> config.cloudRenderDistance = v)
                        .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(128, 768).step(64))
                        .build())
                .option(bool("weather_seen_coming", true,
                        () -> config.distantStorms, v -> config.distantStorms = v))
                .build();

        // WIND LINES
        ConfigCategory windLines = ConfigCategory.createBuilder()
                .name(Component.translatable("advancedweather.config.category.wind"))
                .option(bool("wind_pushes_particles", true,
                        () -> config.windPushesParticles, v -> config.windPushesParticles = v))
                .option(bool("foliage_sway", true,
                        () -> config.foliageSway, v -> config.foliageSway = v))
                .option(Option.<Double>createBuilder()
                        .name(Component.translatable("advancedweather.config.foliage_sway_strength"))
                        .binding(1.0, () -> config.foliageSwayStrength, v -> config.foliageSwayStrength = v)
                        .controller(opt -> DoubleFieldControllerBuilder.create(opt).min(0.0).max(3.0))
                        .build())
                .option(Option.<Double>createBuilder()
                        .name(Component.translatable("advancedweather.config.wind_particle_strength"))
                        .binding(1.0, () -> config.windParticleStrength, v -> config.windParticleStrength = v)
                        .controller(opt -> DoubleFieldControllerBuilder.create(opt).min(0.0).max(5.0))
                        .build())
                .option(Option.<Integer>createBuilder()
                        .name(Component.translatable("advancedweather.config.max_simultaneous_wind_lines"))
                        .description(OptionDescription.of(Component.translatable("advancedweather.config.max_simultaneous_wind_lines.desc")))
                        .binding(80, () -> config.windLineMax, v -> config.windLineMax = v)
                        .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(10, 300).step(1))
                        .build())
                .option(Option.<Double>createBuilder()
                        .name(Component.translatable("advancedweather.config.min_wind_line_length"))
                        .binding(1.5, () -> config.windLineLengthMin, v -> config.windLineLengthMin = v)
                        .controller(DoubleFieldControllerBuilder::create)
                        .build())
                .option(Option.<Double>createBuilder()
                        .name(Component.translatable("advancedweather.config.max_wind_line_length"))
                        .binding(4.5, () -> config.windLineLengthMax, v -> config.windLineLengthMax = v)
                        .controller(DoubleFieldControllerBuilder::create)
                        .build())
                .option(Option.<Double>createBuilder()
                        .name(Component.translatable("advancedweather.config.min_spawn_distance"))
                        .binding(15.0, () -> config.windLineSpawnDistMin, v -> config.windLineSpawnDistMin = v)
                        .controller(DoubleFieldControllerBuilder::create)
                        .build())
                .option(Option.<Double>createBuilder()
                        .name(Component.translatable("advancedweather.config.max_spawn_distance"))
                        .binding(25.0, () -> config.windLineSpawnDistMax, v -> config.windLineSpawnDistMax = v)
                        .controller(DoubleFieldControllerBuilder::create)
                        .build())
                .option(Option.<Double>createBuilder()
                        .name(Component.translatable("advancedweather.config.lateral_spread"))
                        .binding(35.0, () -> config.windLineLateralSpread, v -> config.windLineLateralSpread = v)
                        .controller(DoubleFieldControllerBuilder::create)
                        .build())
                .option(Option.<Double>createBuilder()
                        .name(Component.translatable("advancedweather.config.height_spread"))
                        .binding(9.0, () -> config.windLineHeightSpread, v -> config.windLineHeightSpread = v)
                        .controller(DoubleFieldControllerBuilder::create)
                        .build())
                .build();

        // BLIZZARD
        ConfigCategory blizzard = ConfigCategory.createBuilder()
                .name(Component.translatable("advancedweather.config.category.blizzard_particles"))
                .option(Option.<Integer>createBuilder()
                        .name(Component.translatable("advancedweather.config.particles_per_batch"))
                        .description(OptionDescription.of(Component.translatable("advancedweather.config.particles_per_batch.desc")))
                        .binding(48, () -> config.blizzardCount, v -> config.blizzardCount = v)
                        .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(1, 200).step(1))
                        .build())
                .option(Option.<Integer>createBuilder()
                        .name(Component.translatable("advancedweather.config.spawn_interval_ticks"))
                        .description(OptionDescription.of(Component.translatable("advancedweather.config.spawn_interval_ticks.desc")))
                        .binding(2, () -> config.blizzardTickInterval, v -> config.blizzardTickInterval = v)
                        .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(1, 40).step(1))
                        .build())
                .option(Option.<Double>createBuilder()
                        .name(Component.translatable("advancedweather.config.spawn_distance"))
                        .binding(8.0, () -> config.blizzardSpawnDist, v -> config.blizzardSpawnDist = v)
                        .controller(DoubleFieldControllerBuilder::create)
                        .build())
                .option(Option.<Double>createBuilder()
                        .name(Component.translatable("advancedweather.config.horizontal_spread"))
                        .binding(16.0, () -> config.blizzardSpread, v -> config.blizzardSpread = v)
                        .controller(DoubleFieldControllerBuilder::create)
                        .build())
                .option(Option.<Double>createBuilder()
                        .name(Component.translatable("advancedweather.config.vertical_spread"))
                        .binding(12.0, () -> config.blizzardHeightSpread, v -> config.blizzardHeightSpread = v)
                        .controller(DoubleFieldControllerBuilder::create)
                        .build())
                .option(Option.<Double>createBuilder()
                        .name(Component.translatable("advancedweather.config.wind_speed"))
                        .binding(0.5, () -> config.blizzardWindSpeed, v -> config.blizzardWindSpeed = v)
                        .controller(DoubleFieldControllerBuilder::create)
                        .build())
                .option(Option.<Double>createBuilder()
                        .name(Component.translatable("advancedweather.config.min_particle_size"))
                        .binding(0.14, () -> config.blizzardSizeMin, v -> config.blizzardSizeMin = v)
                        .controller(DoubleFieldControllerBuilder::create)
                        .build())
                .option(Option.<Double>createBuilder()
                        .name(Component.translatable("advancedweather.config.max_particle_size"))
                        .binding(0.25, () -> config.blizzardSizeMax, v -> config.blizzardSizeMax = v)
                        .controller(DoubleFieldControllerBuilder::create)
                        .build())
                .build();

        // SAND PARTICLES
        ConfigCategory sandParticles = ConfigCategory.createBuilder()
                .name(Component.translatable("advancedweather.config.category.sand_particles"))
                .option(Option.<Integer>createBuilder()
                        .name(Component.translatable("advancedweather.config.sand_particles.particles_per_batch"))
                        .description(OptionDescription.of(Component.translatable("advancedweather.config.sand_particles.particles_per_batch.desc")))
                        .binding(48, () -> config.sandCount, v -> config.sandCount = v)
                        .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(1, 200).step(1))
                        .build())
                .option(Option.<Integer>createBuilder()
                        .name(Component.translatable("advancedweather.config.sand_particles.spawn_interval_ticks"))
                        .description(OptionDescription.of(Component.translatable("advancedweather.config.sand_particles.spawn_interval_ticks.desc")))
                        .binding(2, () -> config.sandTickInterval, v -> config.sandTickInterval = v)
                        .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(1, 40).step(1))
                        .build())
                .option(Option.<Double>createBuilder()
                        .name(Component.translatable("advancedweather.config.spawn_distance"))
                        .binding(8.0, () -> config.sandSpawnDist, v -> config.sandSpawnDist = v)
                        .controller(DoubleFieldControllerBuilder::create)
                        .build())
                .option(Option.<Double>createBuilder()
                        .name(Component.translatable("advancedweather.config.horizontal_spread"))
                        .binding(16.0, () -> config.sandSpread, v -> config.sandSpread = v)
                        .controller(DoubleFieldControllerBuilder::create)
                        .build())
                .option(Option.<Double>createBuilder()
                        .name(Component.translatable("advancedweather.config.vertical_spread"))
                        .binding(8.0, () -> config.sandHeightSpread, v -> config.sandHeightSpread = v)
                        .controller(DoubleFieldControllerBuilder::create)
                        .build())
                .option(Option.<Double>createBuilder()
                        .name(Component.translatable("advancedweather.config.wind_speed"))
                        .binding(0.8, () -> config.sandWindSpeed, v -> config.sandWindSpeed = v)
                        .controller(DoubleFieldControllerBuilder::create)
                        .build())
                .option(Option.<Double>createBuilder()
                        .name(Component.translatable("advancedweather.config.min_particle_size"))
                        .binding(0.22, () -> config.sandSizeMin, v -> config.sandSizeMin = v)
                        .controller(DoubleFieldControllerBuilder::create)
                        .build())
                .option(Option.<Double>createBuilder()
                        .name(Component.translatable("advancedweather.config.max_particle_size"))
                        .binding(0.27, () -> config.sandSizeMax, v -> config.sandSizeMax = v)
                        .controller(DoubleFieldControllerBuilder::create)
                        .build())
                .build();

        // BRIMSTONE PARTICLES
        ConfigCategory brimstoneParticles = ConfigCategory.createBuilder()
                .name(Component.translatable("advancedweather.config.category.brimstone_particles"))
                .option(Option.<Integer>createBuilder()
                        .name(Component.translatable("advancedweather.config.brimstone_particles.particles_per_batch"))
                        .description(OptionDescription.of(Component.translatable("advancedweather.config.brimstone_particles.particles_per_batch.desc")))
                        .binding(48, () -> config.brimstoneCount, v -> config.brimstoneCount = v)
                        .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(1, 200).step(1))
                        .build())
                .option(Option.<Integer>createBuilder()
                        .name(Component.translatable("advancedweather.config.brimstone_particles.spawn_interval_ticks"))
                        .description(OptionDescription.of(Component.translatable("advancedweather.config.brimstone_particles.spawn_interval_ticks.desc")))
                        .binding(2, () -> config.brimstoneTickInterval, v -> config.brimstoneTickInterval = v)
                        .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(1, 40).step(1))
                        .build())
                .option(Option.<Double>createBuilder()
                        .name(Component.translatable("advancedweather.config.spawn_distance"))
                        .binding(8.0, () -> config.brimstoneSpawnDist, v -> config.brimstoneSpawnDist = v)
                        .controller(DoubleFieldControllerBuilder::create)
                        .build())
                .option(Option.<Double>createBuilder()
                        .name(Component.translatable("advancedweather.config.horizontal_spread"))
                        .binding(16.0, () -> config.brimstoneSpread, v -> config.brimstoneSpread = v)
                        .controller(DoubleFieldControllerBuilder::create)
                        .build())
                .option(Option.<Double>createBuilder()
                        .name(Component.translatable("advancedweather.config.vertical_spread"))
                        .binding(8.0, () -> config.brimstoneHeightSpread, v -> config.brimstoneHeightSpread = v)
                        .controller(DoubleFieldControllerBuilder::create)
                        .build())
                .option(Option.<Double>createBuilder()
                        .name(Component.translatable("advancedweather.config.wind_speed"))
                        .binding(0.8, () -> config.brimstoneWindSpeed, v -> config.brimstoneWindSpeed = v)
                        .controller(DoubleFieldControllerBuilder::create)
                        .build())
                .option(Option.<Double>createBuilder()
                        .name(Component.translatable("advancedweather.config.min_particle_size"))
                        .binding(0.22, () -> config.brimstoneSizeMin, v -> config.brimstoneSizeMin = v)
                        .controller(DoubleFieldControllerBuilder::create)
                        .build())
                .option(Option.<Double>createBuilder()
                        .name(Component.translatable("advancedweather.config.max_particle_size"))
                        .binding(0.27, () -> config.brimstoneSizeMax, v -> config.brimstoneSizeMax = v)
                        .controller(DoubleFieldControllerBuilder::create)
                        .build())
                .build();

        // GROUND FOG
        ConfigCategory groundFog = ConfigCategory.createBuilder()
                .name(Component.translatable("advancedweather.config.category.ground_fog"))
                .option(Option.<Double>createBuilder()
                        .name(Component.translatable("advancedweather.config.min_opacity"))
                        .binding(0.1, () -> config.groundFogAlphaMin, v -> config.groundFogAlphaMin = v)
                        .controller(DoubleFieldControllerBuilder::create)
                        .build())
                .option(Option.<Double>createBuilder()
                        .name(Component.translatable("advancedweather.config.max_opacity"))
                        .binding(0.35, () -> config.groundFogAlphaMax, v -> config.groundFogAlphaMax = v)
                        .controller(DoubleFieldControllerBuilder::create)
                        .build())
                .option(Option.<Double>createBuilder()
                        .name(Component.translatable("advancedweather.config.min_size"))
                        .binding(0.6, () -> config.groundFogSizeMin, v -> config.groundFogSizeMin = v)
                        .controller(DoubleFieldControllerBuilder::create)
                        .build())
                .option(Option.<Double>createBuilder()
                        .name(Component.translatable("advancedweather.config.max_size"))
                        .binding(1.7, () -> config.groundFogSizeMax, v -> config.groundFogSizeMax = v)
                        .controller(DoubleFieldControllerBuilder::create)
                        .build())
                .option(Option.<Double>createBuilder()
                        .name(Component.translatable("advancedweather.config.width_to_height_ratio"))
                        .description(OptionDescription.of(Component.translatable("advancedweather.config.width_to_height_ratio.desc")))
                        .binding(3.5, () -> config.groundFogWidthScale, v -> config.groundFogWidthScale = v)
                        .controller(DoubleFieldControllerBuilder::create)
                        .build())
                .option(Option.<Integer>createBuilder()
                        .name(Component.translatable("advancedweather.config.min_lifetime_ticks"))
                        .binding(300, () -> config.groundFogLifetimeMin, v -> config.groundFogLifetimeMin = v)
                        .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(20, 2000).step(1))
                        .build())
                .option(Option.<Integer>createBuilder()
                        .name(Component.translatable("advancedweather.config.max_lifetime_ticks"))
                        .binding(500, () -> config.groundFogLifetimeMax, v -> config.groundFogLifetimeMax = v)
                        .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(20, 2000).step(1))
                        .build())
                .build();

        ConfigCategory freezingRain = ConfigCategory.createBuilder()
                .name(Component.translatable("advancedweather.config.category.frost_snow_and_sand"))
                .option(bool("frozen_ground", true,
                        () -> config.frozenGround, v -> config.frozenGround = v))
                .option(bool("snow_cover", true,
                        () -> config.snowCover, v -> config.snowCover = v))
                .option(bool("snow_cover_over_snow_blocks", false,
                        () -> config.snowCoverOnSnow, v -> config.snowCoverOnSnow = v))
                .option(bool("sand_cover", true,
                        () -> config.sandCover, v -> config.sandCover = v))
                .build();

        ConfigCategory particleRainCompat = ConfigCategory.createBuilder()
                .name(Component.translatable("advancedweather.config.category.particle_rain_compatibility"))
                .option(bool("enable_particle_rain_integration", true,
                        () -> config.particleRainIntegration, v -> config.particleRainIntegration = v))
                .build();

        ConfigCategory atmosphericsCompat = ConfigCategory.createBuilder()
                .name(Component.translatable("advancedweather.config.category.atmospherics_compatibility"))
                .option(bool("enable_atmospherics_integration", true,
                        () -> config.atmosphericsIntegration, v -> config.atmosphericsIntegration = v))
                .build();

        // CLIENT categories
        builder.category(weatherHud);
        builder.category(units);
        builder.category(clouds);
        builder.category(windLines);
        builder.category(blizzard);
        builder.category(sandParticles);
        builder.category(brimstoneParticles);
        builder.category(groundFog);
        builder.category(freezingRain);
        builder.category(particleRainCompat);
        builder.category(atmosphericsCompat);

        return builder.build().generateScreen(parent);
    }

    // COMMON
    public static Screen createCommon(Screen parent) {
        AWCommonConfig common = AWCommonConfig.get();

        YetAnotherConfigLib.Builder builder = YetAnotherConfigLib.createBuilder()
                .title(Component.translatable("advancedweather.config.title.common"))
                .save(common::save);

        // WEATHER TYPES
        ConfigCategory weatherTypes = ConfigCategory.createBuilder()
                .name(Component.translatable("advancedweather.config.category.weather_types"))
                .group(OptionGroup.createBuilder()
                        .name(Component.translatable("advancedweather.config.group.overworld"))
                        .option(bool("fog", true, () -> common.enableFog, v -> common.enableFog = v))
                        .option(bool("dense_fog", true, () -> common.enableDenseFog, v -> common.enableDenseFog = v))
                        .option(bool("mist", true, () -> common.enableMist, v -> common.enableMist = v))
                        .option(bool("sandstorm", true, () -> common.enableSandstorm, v -> common.enableSandstorm = v))
                        .option(bool("blizzard", true, () -> common.enableBlizzard, v -> common.enableBlizzard = v))
                        .option(bool("freezing_rain", true, () -> common.enableFreezingRain, v -> common.enableFreezingRain = v))
                        .option(bool("hail", true, () -> common.enableHail, v -> common.enableHail = v))
                        .option(bool("thunderstorm", true, () -> common.enableThunderstorm, v -> common.enableThunderstorm = v))
                        .build())
                .group(OptionGroup.createBuilder()
                        .name(Component.translatable("advancedweather.config.group.nether"))
                        .option(bool("nether_weather", true, () -> common.enableNetherWeather, v -> common.enableNetherWeather = v))
                        .option(bool("ash_storm", true, () -> common.enableAshStorm, v -> common.enableAshStorm = v))
                        .option(bool("brimstone_storm", true, () -> common.enableBrimstoneStorm, v -> common.enableBrimstoneStorm = v))
                        .option(bool("lava_rain", true, () -> common.enableLavaRain, v -> common.enableLavaRain = v))
                        .option(bool("netherstorm", true, () -> common.enableNetherstorm, v -> common.enableNetherstorm = v))
                        .option(bool("hellfire", true, () -> common.enableHellfire, v -> common.enableHellfire = v))
                        .build())
                .group(OptionGroup.createBuilder()
                        .name(Component.translatable("advancedweather.config.group.the_end"))
                        .option(bool("end_weather", true, () -> common.enableEndWeather, v -> common.enableEndWeather = v))
                        .option(bool("void_storm", true, () -> common.enableVoidStorm, v -> common.enableVoidStorm = v))
                        .option(bool("end_mist", true, () -> common.enableEndMist, v -> common.enableEndMist = v))
                        .option(bool("chorus_gale", true, () -> common.enableChorusGale, v -> common.enableChorusGale = v))
                        .option(bool("enderstorm", true, () -> common.enableEnderstorm, v -> common.enableEnderstorm = v))
                        .build())
                .build();

        // GAMEPLAY
        ConfigCategory gameplay = ConfigCategory.createBuilder()
                .name(Component.translatable("advancedweather.config.category.gameplay"))
                .option(bool("hail_damages_crops", true, () -> common.hailDamageCrops, v -> common.hailDamageCrops = v))
                .option(bool("hail_damages_entities", true, () -> common.hailDamageEntities, v -> common.hailDamageEntities = v))
                .option(bool("lava_rain_burns_entities", true,
                        () -> common.lavaRainBurnsEntities, v -> common.lavaRainBurnsEntities = v))
                .option(bool("void_storm_updrafts", true,
                        () -> common.voidStormUpdrafts, v -> common.voidStormUpdrafts = v))
                .option(bool("hail_breaks_glass", true, () -> common.hailBreakGlass, v -> common.hailBreakGlass = v))
                .option(bool("heavy_rain_slows_crops", true, () -> common.heavyRainSlowCrops, v -> common.heavyRainSlowCrops = v))
                .option(bool("rain_boosts_crops", true, () -> common.rainBoostsCrops, v -> common.rainBoostsCrops = v))
                .option(bool("sun_boosts_crops", true, () -> common.sunBoostsCrops, v -> common.sunBoostsCrops = v))
                .option(bool("frost_kills_crops", true, () -> common.frostKillsCrops, v -> common.frostKillsCrops = v))
                .option(Option.<Double>createBuilder()
                        .name(Component.translatable("advancedweather.config.snow_cover_build_up_minutes"))
                        .description(OptionDescription.of(Component.translatable("advancedweather.config.snow_cover_build_up_minutes.desc")))
                        .binding(4.0, () -> common.snowCoverMinutes, v -> common.snowCoverMinutes = v)
                        .controller(opt -> DoubleFieldControllerBuilder.create(opt).min(0.5).max(60.0))
                        .build())
                .option(bool("drought_dries_farmland", true, () -> common.droughtDriesFarmland, v -> common.droughtDriesFarmland = v))
                .option(bool("wind_spreads_fire", true, () -> common.windSpreadsFire, v -> common.windSpreadsFire = v))
                .option(bool("sandstorm_affects_players", true, () -> common.sandstormAffectsPlayers, v -> common.sandstormAffectsPlayers = v))
                .option(bool("rainbow_brings_luck", true, () -> common.rainbowBringsLuck, v -> common.rainbowBringsLuck = v))
                .option(bool("fog_hides_players", true, () -> common.fogHidesPlayers, v -> common.fogHidesPlayers = v))
                .option(bool("dense_fog_brings_monsters", true, () -> common.denseFogSpawnsMonsters, v -> common.denseFogSpawnsMonsters = v))
                .option(bool("station_requires_sensors", true, () -> common.stationRequiresSensors, v -> common.stationRequiresSensors = v))
                .option(Option.<Double>createBuilder()
                        .name(Component.translatable("advancedweather.config.sensor_link_max_distance"))
                        .description(OptionDescription.of(Component.translatable("advancedweather.config.sensor_link_max_distance.desc")))
                        .binding(32.0, () -> common.sensorLinkMaxDistance, v -> common.sensorLinkMaxDistance = v)
                        .controller(opt -> DoubleFieldControllerBuilder.create(opt).min(1.0).max(256.0))
                        .build())
                .build();

        // REAL WEATHER
        ConfigCategory realWeather = ConfigCategory.createBuilder()
                .name(Component.translatable("advancedweather.config.category.real_weather"))
                .option(bool("enable_real_world_weather",
                        false, () -> common.realWeatherEnabled, v -> common.realWeatherEnabled = v))
                .option(Option.<Double>createBuilder()
                        .name(Component.translatable("advancedweather.config.latitude"))
                        .binding(48.8566, () -> common.latitude, v -> common.latitude = v)
                        .controller(opt -> DoubleFieldControllerBuilder.create(opt).min(-90.0).max(90.0))
                        .build())
                .option(Option.<Double>createBuilder()
                        .name(Component.translatable("advancedweather.config.longitude"))
                        .binding(2.3522, () -> common.longitude, v -> common.longitude = v)
                        .controller(opt -> DoubleFieldControllerBuilder.create(opt).min(-180.0).max(180.0))
                        .build())
                .option(Option.<Integer>createBuilder()
                        .name(Component.translatable("advancedweather.config.sync_interval_minutes"))
                        .binding(4, () -> common.syncIntervalMinutes, v -> common.syncIntervalMinutes = v)
                        .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(1, 120).step(1))
                        .build())
                .option(bool("real_weather_overrides_commands", false,
                        () -> common.realWeatherOverridesCommands, v -> common.realWeatherOverridesCommands = v))
                .build();

        // REPORTS & ARCHIVE
        ConfigCategory archive = ConfigCategory.createBuilder()
                .name(Component.translatable("advancedweather.config.category.reports_archive"))
                .option(bool("enable_weather_reports", true, () -> common.enableWeatherReports, v -> common.enableWeatherReports = v))
                .option(bool("reject_older_reports", true, () -> common.archiveRejectOlderReports, v -> common.archiveRejectOlderReports = v))
                .option(Option.<Integer>createBuilder()
                        .name(Component.translatable("advancedweather.config.archive_max_records"))
                        .binding(20000, () -> common.archiveMaxRecords, v -> common.archiveMaxRecords = v)
                        .controller(opt -> IntegerFieldControllerBuilder.create(opt).min(1))
                        .build())
                .option(Option.<Integer>createBuilder()
                        .name(Component.translatable("advancedweather.config.min_records_for_forecast"))
                        .binding(15, () -> common.archiveMinRecordsForForecast, v -> common.archiveMinRecordsForForecast = v)
                        .controller(opt -> IntegerFieldControllerBuilder.create(opt).min(0))
                        .build())
                .option(Option.<Integer>createBuilder()
                        .name(Component.translatable("advancedweather.config.confidence_floor_records"))
                        .binding(15, () -> common.archiveConfidenceFloorRecords, v -> common.archiveConfidenceFloorRecords = v)
                        .controller(opt -> IntegerFieldControllerBuilder.create(opt).min(0))
                        .build())
                .option(Option.<Integer>createBuilder()
                        .name(Component.translatable("advancedweather.config.confidence_saturation_records"))
                        .binding(200, () -> common.archiveConfidenceSaturationRecords, v -> common.archiveConfidenceSaturationRecords = v)
                        .controller(opt -> IntegerFieldControllerBuilder.create(opt).min(1))
                        .build())
                .option(Option.<Integer>createBuilder()
                        .name(Component.translatable("advancedweather.config.almanac_max_records"))
                        .binding(32, () -> common.almanacMaxRecords, v -> common.almanacMaxRecords = v)
                        .controller(opt -> IntegerFieldControllerBuilder.create(opt).min(1))
                        .build())
                .option(Option.<Integer>createBuilder()
                        .name(Component.translatable("advancedweather.config.report_printing_time_ticks"))
                        .binding(30, () -> common.reportPrintingTicks, v -> common.reportPrintingTicks = v)
                        .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(1, 200).step(1))
                        .build())
                .option(Option.<Long>createBuilder()
                        .name(Component.translatable("advancedweather.config.forecast_horizon_ticks"))
                        .binding(1200L, () -> common.archiveForecastHorizonTicks, v -> common.archiveForecastHorizonTicks = v)
                        .controller(LongFieldControllerBuilder::create)
                        .build())
                .option(Option.<Long>createBuilder()
                        .name(Component.translatable("advancedweather.config.aging_data_threshold_ticks"))
                        .binding(12000L, () -> common.archiveAgingDataTicks, v -> common.archiveAgingDataTicks = v)
                        .controller(LongFieldControllerBuilder::create)
                        .build())
                .option(Option.<Long>createBuilder()
                        .name(Component.translatable("advancedweather.config.outdated_data_threshold_ticks"))
                        .binding(24000L, () -> common.archiveOutdatedDataTicks, v -> common.archiveOutdatedDataTicks = v)
                        .controller(LongFieldControllerBuilder::create)
                        .build())
                .build();

        // CREATE COMPAT
        ConfigCategory createCompat = ConfigCategory.createBuilder()
                .name(Component.translatable("advancedweather.config.category.create_compatibility"))
                .option(bool("enable_windmill_compat", true, () -> common.enableCreateWindmillCompat, v -> common.enableCreateWindmillCompat = v))
                .option(bool("wind_direction_affects_sail_speed", true, () -> common.windmillDirectionAffectsOutput, v -> common.windmillDirectionAffectsOutput = v))
                .option(Option.<Double>createBuilder()
                        .name(Component.translatable("advancedweather.config.windmill_wind_threshold"))
                        .binding(0.4, () -> common.windmillWindThreshold, v -> common.windmillWindThreshold = v)
                        .controller(opt -> DoubleFieldControllerBuilder.create(opt).min(0.0).max(1.0))
                        .build())
                .option(Option.<Double>createBuilder()
                        .name(Component.translatable("advancedweather.config.windmill_speed_scale_factor"))
                        .binding(0.025, () -> common.windmillSpeedScaleFactor, v -> common.windmillSpeedScaleFactor = v)
                        .controller(opt -> DoubleFieldControllerBuilder.create(opt).min(0.0).max(5.0))
                        .build())
                .option(bool("wind_pushes_contraptions", true,
                        () -> common.windAffectsContraptions, v -> common.windAffectsContraptions = v))
                .option(Option.<Double>createBuilder()
                        .name(Component.translatable("advancedweather.config.contraption_wind_strength"))
                        .binding(1.0, () -> common.contraptionWindStrength, v -> common.contraptionWindStrength = v)
                        .controller(opt -> DoubleFieldControllerBuilder.create(opt).min(0.0).max(5.0))
                        .build())
                .build();

        ConfigCategory coldSweatCompat = ConfigCategory.createBuilder()
                .name(Component.translatable("advancedweather.config.category.cold_sweat_compatibility"))
                .option(bool("enable_cold_sweat_integration", true,
                        () -> common.coldSweatIntegration, v -> common.coldSweatIntegration = v))
                .option(bool("wind_chill_humidity", true,
                        () -> common.coldSweatWindChill, v -> common.coldSweatWindChill = v))
                .option(bool("replace_cold_sweat_s_weather2_storm_effect", true,
                        () -> common.coldSweatReplaceStormModifier, v -> common.coldSweatReplaceStormModifier = v))
                .option(Option.<Double>createBuilder()
                        .name(Component.translatable("advancedweather.config.weather_temperature_strength"))
                        .binding(0.75, () -> common.coldSweatWeatherStrength, v -> common.coldSweatWeatherStrength = v)
                        .controller(opt -> DoubleFieldControllerBuilder.create(opt).min(0.0).max(3.0))
                        .build())
                .build();

        builder.category(weatherTypes);
        builder.category(gameplay);
        builder.category(realWeather);
        builder.category(archive);
        builder.category(createCompat);
        builder.category(coldSweatCompat);

        return builder.build().generateScreen(parent);
    }

    private static Option<Boolean> bool(String id, boolean def, Supplier<Boolean> getter, Consumer<Boolean> setter) {
        String key = "advancedweather.config." + id;
        Option.Builder<Boolean> b = Option.<Boolean>createBuilder()
                .name(Component.translatable(key));
        if (I18n.exists(key + ".desc")) {
            b.description(OptionDescription.of(Component.translatable(key + ".desc")));
        }
        return b.binding(def, getter, setter)
                .controller(BooleanControllerBuilder::create)
                .build();
    }
}
