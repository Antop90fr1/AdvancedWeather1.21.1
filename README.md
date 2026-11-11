# Advanced Weather

*A physically-grounded weather overhaul for Minecraft: 28 weather types across all three dimensions, a real atmospheric simulation, and a full progression of craftable weather instruments.*

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-brightgreen)]()
[![Loader](https://img.shields.io/badge/Loader-NeoForge-orange)]()
[![License](https://img.shields.io/badge/License-MIT-blue)](LICENSE.txt)

<p align="center">
  <img src="docs/images/banner.png" alt="Advanced Weather" width="100%">
</p>

---

## What is Advanced Weather?

Vanilla Minecraft has three weather states: clear, rain, and thunder. Advanced Weather replaces that with a living atmosphere. Air pressure, temperature, dew point and wind are simulated continuously and drive **28 distinct weather types**, each with its own particles, fog, sky, sound and gameplay effects. Weather doesn't teleport between states: it *transitions* based on the current atmospheric conditions, and you can actually **read, predict and archive** it using an in-world progression of scientific instruments you build yourself.

You can let it run procedurally, sync it to **real-world weather** at any location on Earth, or take full manual control.

---

## Features

### 🌍 A real atmospheric simulation

Runs a continuous model:

- **Air pressure** driven by layered simplex noise, with realistic velocity, damping and gusting.
- **Temperature** derived from the weather type, the solar day/night cycle, adiabatic pressure effects and (optionally) the current season.
- **Humidity** computed from temperature and dew point using the Magnus relative-humidity formula.
- **Wind** with base intensity per weather type, pressure-gradient contribution and randomized gusts.
- **Probabilistic transitions** - the next weather is chosen from a transition graph weighted by live atmospheric conditions, so a falling barometer genuinely means a storm is coming.
- **Forecasting** - the engine projects the atmosphere forward in time to predict the *next* weather and the weather *in 30 minutes*, each with a confidence score.

Everything is per-dimension and saved with the world.

### 🌦️ 28 weather types across three dimensions
## **Overworld (17):** 
#### Clear · Sunny · Cloudy · Overcast · Mist · Fog · Dense Fog · Drizzle · Light Rain · Heavy Rain · Freezing Rain · Thunderstorm · Snow · Blizzard · Hail · Windy · Sandstorm
<p>
  <img src="docs/images/heavy_rain.png" alt="Heavy Rain" width="75%">
</p>
<p>
  <img src="docs/images/sandstorm.png" alt="Sandstorm" width="75%">
</p>
<p>
  <img src="docs/images/blizzard.png" alt="Blizzard" width="75%">
</p>

## **Nether (6):** 
#### Nether Clear · Ash Storm · Brimstone Storm · Lava Rain · Netherstorm · Hellfire
<p>
  <img src="docs/images/hellfire.png" alt="Hellfire" width="75%">
</p>
<p>
  <img src="docs/images/lava_rain.png" alt="Lava Rain" width="75%">
</p>

## **The End (5):** 
#### End Clear · End Mist · Void Storm · Chorus Gale · Enderstorm
<p>
  <img src="docs/images/void_storm.png" alt="Void Storm" width="75%">
</p>
<p>
  <img src="docs/images/enderstorm.png" alt="Enderstorm" width="75%">
</p>

A configurable HUD shows which weather type is active, along with the time of day (can be disabled in the config):
<p>
  <img src="docs/images/weather_hud.png" alt="Weather HUD">
</p>


### ✨ Atmospheric effects

Each weather brings its own hand-tuned visuals and audio:

- Volumetric **wind lines** and directional wind sound that scale with intensity and gusts.
- Weather-specific particles: blizzard flakes, sand, ash, ground fog, freezing splashes, hail, ember/void particles.
- **Rainbows** that can spawn after rain clears or during drizzle, **heat shimmer** in extreme heat, **water and lava ripples**, wind-blown **tumbleweeds** and drifting **chorus plants**.
- Dynamic **fog, sky, cloud, sun, star and lightning** rendering that reacts to the current weather.

### ⚡ Weather that affects gameplay

- **Hail** can damage crops, hurt exposed entities and break glass (each toggleable).
- **Heavy rain** slows crop growth.
- **Blizzards** deepen snow accumulation.
- Mobs **seek shelter** from dangerous weather.
- Lightning behaviour is reworked for storms.

*(All gameplay impacts are individually configurable - see [Configuration](#configuration).)*

### 🔬 Instruments & progression

Advanced Weather is also a **tech-progression mod**. You can't just read the weather, you have to build the tools.

- **New ores & materials:** Cinnabar (→ washed dust → distilled into **Mercury**) and Bauxite (→ Alumina → carbothermic reduction → **Aluminum**, ingots, nuggets and sheets). A **Hammer** lets you crush and press by hand (or use Create's crushing wheels).
- **Four core instruments**, each built from a calibrated heart: **Thermometer** (mercury vial), **Barometer** (spring), **Hygrometer** (sensitive fiber), **Anemometer** (cup rotor).
- **Calibration Bench** and **Calibration Tool** - components must be calibrated before assembly, and sensors linked to stations.
- **Portable** versions of every instrument for readings on the move.
<p>
  <img src="docs/images/calibration_bench.png" alt="Calibration Bench" width="75%">
</p>

### 📟 The weather station network

Build a working meteorological station:

- **Weather Station** - displays live readings; link sensors to unlock each measurement. Right-click with paper to **print a Weather Report**; rename it to your liking.
- **Sensors** (Thermometer / Barometer / Hygrometer / Anemometer) with **realistic placement rules** - a thermometer must be sheltered from direct sky with airflow; an anemometer needs open sky and clear sides; etc. Bad placement gives bad readings.
- **Weather Archive** - store reports and almanacs; it studies your measurement history to **forecast the next weather**, rejecting duplicates and reports older than its memory.
- **Auto-Sampler** - automatically feeds station data into an archive at a configurable interval.
- **Weather Detector** - emits redstone based on weather (rain, snow, storm, thunderstorm, hail, or an analog 0–15 wind signal). With early warning on, it lights up 90 s before the watched weather arrives and stays on while it lasts.
- **Weather Almanac** - a portable record of archived trends.

Two keybinds open a **weather history graph** and a **probable-transitions view** so you can study the atmosphere directly (only on debug mode).
<p>
  <img src="docs/images/machines.png" alt="Machines" width="75%">
</p>
<p>
  <img src="docs/images/sensors.png" alt="Sensors" width="75%">
</p>


### 🌐 Real-world weather mode *(opt-in)*

Enable it in the config and Advanced Weather will pull **live weather for any latitude/longitude on Earth** (via Open-Meteo) and reverse-geocode the place name (via OpenStreetMap Nominatim), mapping real conditions onto Minecraft's weather system.

> **Privacy note:** this mode is **disabled by default**. When enabled, the coordinates you set in the config are sent to Open-Meteo and OpenStreetMap Nominatim to fetch weather and a location name. No data leaves your game unless you turn this on.

---

## Configuration

Config files are generated in your `config/` folder (`advancedweather-common.json` and the client config). Highlights:

- **Weather toggles** - enable/disable any weather type per dimension (Overworld, Nether, End).
- **Gameplay** - `hailDamageCrops`, `hailDamageEntities`, `hailBreakGlass`, `heavyRainSlowCrops`.
- **Instruments** - `stationRequiresSensors`, `sensorLinkMaxDistance`.
- **Archive/forecasting** - record capacity, minimum records for a forecast, confidence curve, forecast horizon, data-aging thresholds.
- **Real weather** - `realWeatherEnabled`, `latitude`, `longitude`, `syncIntervalMinutes`, `realWeatherOverridesCommands`.
- **Create compat** - `enableCreateWindmillCompat`, `windmillWindThreshold`, `windmillDirectionAffectsOutput`, `windmillSpeedScaleFactor`.

A **YACL config screen** is provided for in-game editing.

---

## Compatibility

| Mod                                 | Type              | What it adds                                                                     |
|-------------------------------------|-------------------|----------------------------------------------------------------------------------|
| **YetAnotherConfigLib (YACL)**      | Required (both)   | Config files and in-game config screen                                           |
| **Create**                          | Optional          | Windmill sails visibly follow the wind (visual only); weather on Display Boards  |
| **Create: Aeronautics / Simulated** | Optional          | Wind pushes simulated contraptions                                               |
| **Serene Seasons**                  | Optional          | Season-aware weather types, temperature & dew point                              |
| **Ecliptic Seasons**                | Optional          | Its solar terms shape AW weather, temperature and dew point; AW drives its rain  |
| **JEI**                             | Optional (client) | Calibration Bench recipes in the recipe viewer                                   |
| **Particle Rain**                   | Optional (client) | Its rain, snow and dust follow AW weather types, their intensity and the wind    |
| **ATMOSPHERICS**                    | Optional (client) | Its biome fog, sky and dust stay; AW weather fog, cloud veil and wind lead       |
| **Iris Shaders**                    | Partial           | With a shader pack on, AW clouds, fog and ground cover give way to the pack      |

<p>
  <img src="docs/images/wind.png" alt="Aeronautics compatibility" width="75%">
</p>

---

## Requirements

- **Minecraft** 1.21.1
- **NeoForge** 21.1.233+
- **YetAnotherConfigLib** 3.6+

---

## Commands

All commands live under `/aw`. Server commands require permission level 2 (operator); the two client commands run locally and need no permissions.

**Read:**

| Command | Description                                                                    |
|---|--------------------------------------------------------------------------------|
| `/aw` | Show the current weather                                                       |
| `/aw history` | Recent weather history                                                         |

**Control:**

| Command | Description |
|---|---|
| `/aw set <type>` | Switch to a weather type now; it lasts a normal spell, then the weather engine carries on |
| `/aw auto` | Hand control back to the weather engine |
| `/aw refresh` | Pull real-world weather now (requires real weather enabled) |
| `/aw setlocation <lat> <lon>` | Set the real-weather coordinates |
| `/aw effect add\|remove <effect>` | Toggle an individual visual effect |
| `/aw force <pressure> <dewpoint> <temp> <seconds>` | Nudge the atmosphere for a duration |
| `/aw force clear` | Clear active atmospheric forcings |
| `/aw rainbow spawn\|clear` | Spawn or remove rainbows |
| `/aw debug` | Toggle the debug overlay |

**Client:**

| Command | Description |
|---|---|
| `/aw reset` | Clear the client's cached weather data |

---

## For developers

Advanced Weather has a small public API under `net.antopfr.advancedweather.api`. It is a set of static methods: there is nothing to register or to create. Everything **outside** that package is internal and may change without notice, apart from the `WeatherTypes` and `AtmosphericForcing.Bias` enums, which the API uses.

**Setting up** - compile against the mod's jar and declare it as an optional dependency:

```groovy
dependencies {
    compileOnly files("libs/advancedweather-0.1.3.jar")
}
```

```toml
[[dependencies.yourmod]]
modId = "advancedweather"
type = "optional"
versionRange = "[0.1.3,)"
ordering = "NONE"
side = "BOTH"
```

Keep your calls in a class of your own, and only touch it when the mod is there:

```java
if (ModList.get().isLoaded("advancedweather")) {
    MyWeatherCompat.init();
}
```

**Server side** - `AdvancedWeatherAPI`, on the server thread, with any `ServerLevel`:

```java
// The weather, and what comes next
WeatherTypes now = AdvancedWeatherAPI.getCurrentWeather(level);
WeatherTypes coming = AdvancedWeatherAPI.getComingWeather(level);   // certain, but null until about 90 s before the change
int left = AdvancedWeatherAPI.getTicksUntilNextWeather(level);      // -1 where real weather drives the level
WeatherForecast forecast = AdvancedWeatherAPI.getForecast(level);   // an estimate: next and in 30 minutes, with confidence

// The air: for the whole level, or at a place with its biome and height
AtmosphereSnapshot air = AdvancedWeatherAPI.getAtmosphere(level);
float temperature = AdvancedWeatherAPI.getLocalTemperature(level, pos);
float wind = AdvancedWeatherAPI.getWindSpeedKmh(level, pos.getCenter());

// What lies on the ground, from 0 to 1
float snow = AdvancedWeatherAPI.getSnowCover(level, pos);
float sand = AdvancedWeatherAPI.getSandCover(level, pos);

// Changing the weather
AdvancedWeatherAPI.setWeather(level, WeatherTypes.THUNDERSTORM);       // at once; false if wrong dimension
AdvancedWeatherAPI.approachWeather(level, WeatherTypes.THUNDERSTORM);  // seen coming first, arrives in 90 s

// Nudging the atmosphere: pressure, dew point and temperature, for one minute here
AdvancedWeatherAPI.applyForcing(level, -30f, 8f, 0f, 20 * 60);         // weighs on the next change
AdvancedWeatherAPI.seedWeather(level, -30f, 8f, 0f, 20 * 60, AtmosphericForcing.Bias.SEEDING); // and brings it within 25 s

AdvancedWeatherAPI.startAutoWeather(level);                            // new natural duration, next weather redrawn
```

A weather set this way is not pinned: it lasts one natural spell, then the engine carries on. Where the Overworld follows real weather, the next sync replaces it. The atmosphere only decides the weather in the Overworld: in the Nether and the End, a forcing moves the readings, not the odds.

**Listening for changes** - `WeatherChangeEvent` on the NeoForge bus, on the server, whatever caused the change:

```java
NeoForge.EVENT_BUS.addListener((WeatherChangeEvent e) -> {
    if (e.getCurrent() == WeatherTypes.BLIZZARD) { /* ... */ }
});
```

**Client side** - `AdvancedWeatherClientAPI`, for HUD add-ons, on the client thread only. It reads what the local player sees:

```java
WeatherTypes shown = AdvancedWeatherClientAPI.getCurrentWeather();
float felt = AdvancedWeatherClientAPI.getTemperature();
float kmh = AdvancedWeatherClientAPI.getLocalWindSpeedKmh();
WeatherTypes storm = AdvancedWeatherClientAPI.getComingWeather();      // only the storms seen coming in the sky
float cover = AdvancedWeatherClientAPI.getSnowCover();
```

Registering **custom weather types** from another mod is not supported yet - planned for a later API revision.

---

## License

Released under the **MIT License**. See [LICENSE](LICENSE.txt).

## Third-Party Software Notices :

This software embeds the following third-party libraries via JarJar:

1. **[Registrate](https://github.com/tterrag1098/Registrate)**
    - Authors: tterrag, Create Mod Team
    - License: [MPL-2.0 License](licenses/REGISTRATE-LICENSE.txt)

2. **[Veil](https://github.com/FoundryMC/Veil)**
    - Authors: Foundry Systems
    - License: [GNU Lesser General Public License v3.0 (LGPL-3.0)](licenses/VEIL-LICENSE.txt)

The respective licenses and copyright notices for these libraries apply.
See included files for full license texts where applicable.

## Credits

Created by **Antopfr**.

Real-world weather data © [Open-Meteo](https://open-meteo.com/). Location names © [OpenStreetMap contributors](https://www.openstreetmap.org/copyright) via Nominatim.