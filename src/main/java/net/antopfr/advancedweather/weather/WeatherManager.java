package net.antopfr.advancedweather.weather;

import net.antopfr.advancedweather.api.WeatherForecast;
import net.antopfr.advancedweather.api.event.WeatherChangeEvent;
import net.antopfr.advancedweather.api.external.NominatimGeocoding;
import net.antopfr.advancedweather.api.external.OpenMeteo;
import net.antopfr.advancedweather.config.AWCommonConfig;
import net.antopfr.advancedweather.content.advancement.AWAdvancements;
import net.antopfr.advancedweather.network.toclient.*;
import net.antopfr.advancedweather.util.AWLogger;
import net.antopfr.advancedweather.weather.effect.EffectManager;
import net.antopfr.advancedweather.weather.wind.WindField;
import net.antopfr.advancedweather.weather.wind.WindState;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class WeatherManager extends SavedData {

    private static final String NAME = "advancedweather";
    private final Random random = new Random();

    // PER DIM
    private final Map<WeatherTypes.Dimension, WeatherTypes> currentWeathers = new EnumMap<>(WeatherTypes.Dimension.class);
    private final Map<WeatherTypes.Dimension, Integer> weatherTimers = new EnumMap<>(WeatherTypes.Dimension.class);
    private final Map<WeatherTypes.Dimension, Long> lastGameTicks = new EnumMap<>(WeatherTypes.Dimension.class);
    private final Map<WeatherTypes.Dimension, WindState> winds = new EnumMap<>(WeatherTypes.Dimension.class);
    private final Map<WeatherTypes.Dimension, AtmosphericSystem> atmospheres = new EnumMap<>(WeatherTypes.Dimension.class);
    private final Map<WeatherTypes.Dimension, WeatherHistory> histories = new EnumMap<>(WeatherTypes.Dimension.class);
    private final Map<WeatherTypes.Dimension, WeatherDurationTracker> durationTrackers = new EnumMap<>(WeatherTypes.Dimension.class);
    private final Map<WeatherTypes.Dimension, Deque<WeatherTypes>> recentHistory = new EnumMap<>(WeatherTypes.Dimension.class);
    private final Map<WeatherTypes.Dimension, SnowPack> snowPacks = new EnumMap<>(WeatherTypes.Dimension.class);
    private final Map<WeatherTypes.Dimension, SandDrift> sandDrifts = new EnumMap<>(WeatherTypes.Dimension.class);
    private final Map<WeatherTypes.Dimension, WeatherTypes> pendingWeathers = new EnumMap<>(WeatherTypes.Dimension.class);
    private final Map<WeatherTypes.Dimension, KnownOutlook> outlooks = new EnumMap<>(WeatherTypes.Dimension.class);
    private final Map<ServerPlayer, float[]> coverTold = new WeakHashMap<>();

    private static final int HISTORY_SIZE = 5;
    private static final int ATMOSPHERE_SYNC_TICKS = 20;
    private static final int OUTLOOK_TICKS_AHEAD = 36000;
    private static final int OUTLOOK_REFRESH_TICKS = 100;

    private record KnownOutlook(long time, WeatherTypes from, WeatherTransitionGraph.Outlook outlook) {}

    private long rainbowStart;
    private long rainbowEnd;

    private static final int SEED_RESPONSE_MIN = 300;
    private static final int SEED_RESPONSE_SPREAD = 200;

    // REAL MODE
    private final Map<WeatherTypes.Dimension, Integer> realWeatherTickTimers = new EnumMap<>(WeatherTypes.Dimension.class);
    private boolean realWeatherWarningLogged = false;

    public WeatherManager() {
        for (WeatherTypes.Dimension dim : WeatherTypes.Dimension.values()) {
            currentWeathers.put(dim, defaultForDimension(dim));
            weatherTimers.put(dim, -1);
            lastGameTicks.put(dim, -1L);
            winds.put(dim, new WindState());
            atmospheres.put(dim, new AtmosphericSystem());
            histories.put(dim, new WeatherHistory());
            durationTrackers.put(dim, new WeatherDurationTracker());
            realWeatherTickTimers.put(dim, 0);
            recentHistory.put(dim, new ArrayDeque<>());
            snowPacks.put(dim, new SnowPack());
            sandDrifts.put(dim, new SandDrift());
        }
    }

    private static WeatherTypes defaultForDimension(WeatherTypes.Dimension dim) {
        return switch (dim) {
            case NETHER -> WeatherTypes.NETHER_CLEAR;
            case END    -> WeatherTypes.END_CLEAR;
            default     -> WeatherTypes.CLEAR;
        };
    }

    public float getWindIntensity(ServerLevel level) { return getWindState(level).intensity(); }
    public WindState getWindState(ServerLevel level) { return winds.get(DimensionProfile.getDimension(level)); }
    public AtmosphericSystem getAtmosphere(ServerLevel level) { return atmospheres.get(DimensionProfile.getDimension(level)); }
    public WeatherHistory getHistory(ServerLevel level) { return histories.get(DimensionProfile.getDimension(level)); }
    public WeatherDurationTracker getDurationTracker(ServerLevel level) { return durationTrackers.get(DimensionProfile.getDimension(level)); }
    public WeatherTypes getCurrentWeather(ServerLevel level) { return currentWeathers.getOrDefault(DimensionProfile.getDimension(level), WeatherTypes.CLEAR); }
    public List<WeatherTypes> getRecentHistory(ServerLevel level) { return new ArrayList<>(recentHistory.get(DimensionProfile.getDimension(level))); }
    public SnowPack getSnowPack(ServerLevel level) { return snowPacks.get(DimensionProfile.getDimension(level)); }
    public SandDrift getSandDrift(ServerLevel level) { return sandDrifts.get(DimensionProfile.getDimension(level)); }

    public WeatherTypes getComingWeather(ServerLevel level) {
        WeatherTypes.Dimension dim = DimensionProfile.getDimension(level);
        return !followsRealWeather(level) && weatherTimers.get(dim) > 0 ? pendingWeathers.get(dim) : null;
    }

    public boolean followsRealWeather(ServerLevel level) {
        return AWCommonConfig.get().realWeatherEnabled && level.dimension().equals(Level.OVERWORLD);
    }

    public int getTicksUntilNextWeather(ServerLevel level) {
        return followsRealWeather(level) ? -1 : weatherTimers.get(DimensionProfile.getDimension(level));
    }

    public AtmosphericSystem.Mode getMode(ServerLevel level) {
        return followsRealWeather(level) ? AtmosphericSystem.Mode.REAL : getAtmosphere(level).getMode();
    }

    public WeatherForecast getForecast(ServerLevel level) {
        WeatherTypes.Dimension dim = DimensionProfile.getDimension(level);
        AtmosphericSystem a = atmospheres.get(dim);
        WeatherTypes current = currentWeathers.get(dim);
        int toDraw = Math.max(0, weatherTimers.get(dim) - StormApproach.LEAD_TICKS);
        AtmosphericSystem atDraw = a.forecastState(toDraw / 1200, level.getGameTime(), level, current);
        WeatherTransitionGraph.Prediction next = WeatherTransitionGraph.mostLikelyNextWithWeights(
                current, atDraw, dim, level, getRecentHistory(level));
        WeatherTransitionGraph.Outlook later = getOutlook(level);
        return new WeatherForecast(next.type(), ForecastConfidence.compute(next.weights(), a),
                later.type(), later.confidence());
    }

    private WeatherTransitionGraph.Outlook getOutlook(ServerLevel level) {
        WeatherTypes.Dimension dim = DimensionProfile.getDimension(level);
        WeatherTypes current = currentWeathers.get(dim);
        long time = level.getGameTime();

        KnownOutlook known = outlooks.get(dim);
        if (known == null || known.from() != current || Math.abs(time - known.time()) >= OUTLOOK_REFRESH_TICKS) {
            known = new KnownOutlook(time, current, WeatherTransitionGraph.outlook(current,
                    Math.max(0, weatherTimers.get(dim)), OUTLOOK_TICKS_AHEAD,
                    atmospheres.get(dim), dim, level, getRecentHistory(level)));
            outlooks.put(dim, known);
        }
        return known.outlook();
    }

    // TICK
    public void tick(ServerLevel level) {
        AWCommonConfig config = AWCommonConfig.get();
        WeatherTypes.Dimension dim = DimensionProfile.getDimension(level);

        WeatherTypes currentWeather = currentWeathers.get(dim);
        AtmosphericSystem atmosphere = atmospheres.get(dim);
        WeatherHistory history = histories.get(dim);
        WeatherDurationTracker durationTracker = durationTrackers.get(dim);
        WindState wind = winds.get(dim);

        long gameTick = level.getGameTime();
        long delta = lastGameTicks.get(dim) == -1L ? 0 : gameTick - lastGameTicks.get(dim);
        lastGameTicks.put(dim, gameTick);

        if (delta > 100) {
            atmosphere.snapToTarget(level, currentWeather);
        }

        // Atmosphere Engine Tick
        atmosphere.tick(level, currentWeather);

        // Wind update
        float windTarget = atmosphere.getWindIntensity();
        WeatherTypes coming = pendingWeathers.get(dim);
        if (coming != null && StormApproach.announces(coming)) {
            windTarget = Math.max(windTarget, StormApproach.gust(coming, weatherTimers.get(dim)));
        }
        wind.approachIntensity(windTarget, 0.02f);
        wind.advance(WindField.prevailingDirection(level, 0f));
        float windIntensity = wind.intensity();
        if (gameTick % SnowPack.STEP == 0) {
            snowPacks.get(dim).tick(level, currentWeather, atmosphere.getTemperature());
            sandDrifts.get(dim).tick(level, currentWeather, wind.intensity());
            for (ServerPlayer player : level.players()) syncGroundCover(player, false);
        }
        if (gameTick % 20 == 0) {
            syncPacketToDimension(level, WindSyncPacket.of(wind));
            setDirty();
        }

        // History recording
        history.tick(gameTick, atmosphere.getPressure(), windIntensity,
                atmosphere.getTemperature(), atmosphere.getHumidity(),
                currentWeather, dim);

        if (gameTick % 200 == 0 && !history.getEntries().isEmpty()) {
            syncPacketToDimension(level, new HistorySyncPacket(List.of(history.getEntries().getLast())));
        }

        // Sync network packets
        if (gameTick % ATMOSPHERE_SYNC_TICKS == 0 && !level.players().isEmpty()) {
            syncAtmosphereToClients(level);
        }

        // Effects
        if (gameTick % 40 == 0) {
            EffectManager.get(level).resync(level);
        }

        // Duration Tracker
        durationTracker.tick(currentWeather);

        // Real weather mode (ONLY OVERWORLD)
        if (config.realWeatherEnabled) {
            if (level.dimension().equals(Level.OVERWORLD)) {
                int realTimer = realWeatherTickTimers.get(dim) - 1;
                if (realTimer <= 0) {
                    realTimer = config.syncIntervalMinutes * 60 * 20;
                    fetchRealWeather(level);
                }
                realWeatherTickTimers.put(dim, realTimer);
                return;
            }
        }

        // Procedural mode
        int ticksRemaining = weatherTimers.get(dim);

        if (ticksRemaining < 0) return;

        ticksRemaining--;
        if (ticksRemaining <= StormApproach.LEAD_TICKS && !pendingWeathers.containsKey(dim)) {
            WeatherTypes drawn = drawNext(level, dim, currentWeather, atmosphere);
            pendingWeathers.put(dim, drawn);
            if (StormApproach.announces(drawn) && ticksRemaining > 0) {
                syncPacketToDimension(level, new StormApproachPacket(gameTick + ticksRemaining, drawn));
            }
        }
        if (ticksRemaining <= 0) {
            WeatherTypes previous = currentWeather;
            WeatherTypes next = pendingWeathers.remove(dim);

            currentWeathers.put(dim, next);
            pushHistory(dim, next);
            weatherTimers.put(dim, WeatherTransitionGraph.randomDuration(next, random, level.getGameTime() % 24000, level));
            this.setDirty();

            applyToLevel(level, next);
            syncToClients(level, next);

            fireWeatherChange(level, dim, previous, next);
        } else {
            weatherTimers.put(dim, ticksRemaining);
        }
    }

    private WeatherTypes drawNext(ServerLevel level, WeatherTypes.Dimension dim, WeatherTypes current,
                                  AtmosphericSystem atmosphere) {
        WeatherTypes next = WeatherTransitionGraph.getTransitions(current).isEmpty()
                ? defaultForDimension(dim)
                : WeatherTransitionGraph.nextWeather(current, random, atmosphere, dim, level,
                        new ArrayList<>(recentHistory.get(dim)));
        return isCompatibleWithLevel(next, level) ? next : defaultForDimension(dim);
    }

    private void forgetPending(ServerLevel level) {
        WeatherTypes dropped = pendingWeathers.remove(DimensionProfile.getDimension(level));
        if (dropped != null && StormApproach.announces(dropped)) {
            syncPacketToDimension(level, new StormApproachPacket(StormApproachPacket.NONE, dropped));
        }
    }

    public void syncStormApproach(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        WeatherTypes.Dimension dim = DimensionProfile.getDimension(level);
        WeatherTypes coming = pendingWeathers.get(dim);
        int remaining = weatherTimers.get(dim);
        boolean announced = coming != null && StormApproach.announces(coming) && remaining > 0;
        PacketDistributor.sendToPlayer(player, announced
                ? new StormApproachPacket(level.getGameTime() + remaining, coming)
                : new StormApproachPacket(StormApproachPacket.NONE, WeatherTypes.CLEAR));
    }

    public void approach(ServerLevel level, WeatherTypes type) {
        forgetPending(level);
        WeatherTypes.Dimension dim = DimensionProfile.getDimension(level);
        int lead = StormApproach.lead(type);
        pendingWeathers.put(dim, type);
        weatherTimers.put(dim, lead);
        setDirty();
        syncPacketToDimension(level, new StormApproachPacket(level.getGameTime() + lead, type));
    }

    // ALL MODES CONTROL
    public void setCurrentWeather(ServerLevel level, WeatherTypes type) {
        forgetPending(level);
        WeatherTypes.Dimension dim = DimensionProfile.getDimension(level);
        WeatherTypes previous = this.currentWeathers.get(dim);
        this.currentWeathers.put(dim, type);
        this.weatherTimers.put(dim, -1);
        this.setDirty();
        atmospheres.get(dim).setManualMode(type, level);
        applyToLevel(level, type);
        syncToClients(level, type);
        fireWeatherChange(level, dim, previous, type);
    }

    public void applyRealWeather(ServerLevel level, WeatherTypes type) {
        forgetPending(level);
        WeatherTypes.Dimension dim = DimensionProfile.getDimension(level);
        WeatherTypes previous = this.currentWeathers.get(dim);
        this.currentWeathers.put(dim, type);
        this.weatherTimers.put(dim, -1);
        this.setDirty();
        atmospheres.get(dim).setRealWeatherMode(type, level);
        applyToLevel(level, type);
        syncToClients(level, type);
        fireWeatherChange(level, dim, previous, type);

        for (ServerPlayer p : level.players()) AWAdvancements.grant(p, "look_outside");
    }

    private static void fireWeatherChange(ServerLevel level, WeatherTypes.Dimension dim,
                                          WeatherTypes previous, WeatherTypes next) {
        if (previous == next) return;
        NeoForge.EVENT_BUS.post(new WeatherChangeEvent(level, dim, previous, next));
    }

    public void startAutoWeather(ServerLevel level) {
        forgetPending(level);
        WeatherTypes.Dimension dim = DimensionProfile.getDimension(level);
        atmospheres.get(dim).setProceduralMode();

        if (followsRealWeather(level)) {
            realWeatherTickTimers.put(dim, 0);
            fetchRealWeather(level);
        }

        WeatherTypes current = currentWeathers.get(dim);
        weatherTimers.put(dim, WeatherTransitionGraph.randomDuration(current, random, level.getGameTime() % 24000, level));
        setDirty();
    }

    // REAL WEATHER FETCH
    private void fetchRealWeather(ServerLevel level) {
        if (!level.dimension().equals(Level.OVERWORLD)) {
            return;
        }

        AWCommonConfig config = AWCommonConfig.get();
        WeatherTypes.Dimension dim = DimensionProfile.getDimension(level);
        OpenMeteo.fetchAsync(config.latitude, config.longitude)
                .thenAccept(type -> level.getServer().execute(() -> {
                    WeatherTypes currentWeather = currentWeathers.get(dim);
                    if (type == null) {
                        if (!realWeatherWarningLogged) {
                            AWLogger.AW.warn("[AdvancedWeather] Real weather unavailable, falling back to procedural.");
                            realWeatherWarningLogged = true;
                        }
                        if (weatherTimers.get(dim) < 0)
                            weatherTimers.put(dim, WeatherTransitionGraph.randomDuration(currentWeather, random, level.getGameTime() % 24000, level));
                        return;
                    }
                    realWeatherWarningLogged = false;
                    if (!config.realWeatherOverridesCommands && weatherTimers.get(dim) < 0) return;
                    if (type != currentWeather) applyRealWeather(level, type);
                }));
        NominatimGeocoding.fetchLocationNameAsync(config.latitude, config.longitude);
    }

    // APPLY
    private void applyToLevel(ServerLevel level, WeatherTypes weather) {
        boolean raining = weather.isVanillaRaining();
        boolean thundering = weather.isVanillaThundering();

        level.setWeatherParameters(
                raining ? 0 : 6000,
                raining ? 6000 : 0,
                raining, thundering);

        level.getGameRules().getRule(GameRules.RULE_SNOW_ACCUMULATION_HEIGHT)
                .set(weather == WeatherTypes.BLIZZARD ? 8 : 1, level.getServer());

        EffectManager.get(level).setEffects(level,
                EnumSet.copyOf(WeatherDefinitions.get(weather).effects));
    }

    public long getRainbowStart() { return rainbowStart; }
    public long getRainbowEnd()   { return rainbowEnd; }

    public void setRainbow(ServerLevel level, long start, long end) {
        rainbowStart = start;
        rainbowEnd = end;
        setDirty();
        syncPacketToDimension(level, new RainbowSyncPacket(start, end));
    }

    // USED BY CLOUD SEEDING
    public void seed(ServerLevel level, AtmosphericForcing.Bias bias,
                     float pressurePush, float dewPointPush, float tempOffset,
                     int forcingDuration) {
        WeatherTypes.Dimension dim = DimensionProfile.getDimension(level);

        atmospheres.get(dim).applyForcing(
                pressurePush, dewPointPush, tempOffset, forcingDuration, bias);

        int remaining = weatherTimers.get(dim);
        if (remaining > SEED_RESPONSE_MIN + SEED_RESPONSE_SPREAD) {
            weatherTimers.put(dim, SEED_RESPONSE_MIN + random.nextInt(SEED_RESPONSE_SPREAD));
            forgetPending(level);
        }

        setDirty();
    }

    public static boolean isCompatibleWithLevel(WeatherTypes type, ServerLevel level) {
        if (level.dimension().equals(Level.NETHER)) return type.isNether();
        if (level.dimension().equals(Level.END))    return type.isEnd();
        return type.isOverworld();
    }

    private void pushHistory(WeatherTypes.Dimension dim, WeatherTypes type) {
        Deque<WeatherTypes> deque = recentHistory.get(dim);
        deque.addLast(type);
        if (deque.size() > HISTORY_SIZE) deque.removeFirst();
    }

    private void syncToClients(ServerLevel level, WeatherTypes weather) {
        syncPacketToDimension(level, new WeatherSyncPacket(weather));
    }

    private void syncAtmosphereToClients(ServerLevel level) {
        WeatherTypes.Dimension dim = DimensionProfile.getDimension(level);
        AtmosphericSystem a = atmospheres.get(dim);
        WeatherTypes currentWeather = currentWeathers.get(dim);
        long time = level.getGameTime();

        AtmosphericSystem forecast = a.forecastState(30, time, level, currentWeather);
        WeatherForecast outlook = getForecast(level);

        List<TransitionProbabilitiesPacket.Entry> probabilities = WeatherTransitionGraph.computeProbabilities(
                currentWeather, a, dim, level, new ArrayList<>(recentHistory.get(dim)));

        syncPacketToDimension(level, new PressureSyncPacket(
                a.getPressure(), a.getPressureVel(),
                forecast.getPressure(),
                a.getCategory(level).name(),
                outlook.predictedNext().name(), outlook.predictedIn30Min().name(),
                getMode(level).name(),
                outlook.confidenceNext(), outlook.confidenceIn30Min()
        ));
        syncPacketToDimension(level, new TemperatureSyncPacket(
                a.getTemperature(), a.forecastTemperatureFromState(forecast, 30, time, currentWeather, level)));
        syncPacketToDimension(level, new HumiditySyncPacket(
                a.getHumidity(), a.forecastHumidityFromState(forecast, currentWeather, 30, time, level)));
        syncPacketToDimension(level, new TransitionProbabilitiesPacket(probabilities));
    }

    public void syncGroundCover(ServerPlayer player, boolean always) {
        ServerLevel level = player.serverLevel();
        float snow = getSnowPack(level).snowAt(level, player.blockPosition());
        SandDrift.Lying sand = getSandDrift(level).lyingAt(level, player.blockPosition());
        float red = sand.red() ? 1f : 0f;
        float[] told = coverTold.get(player);
        if (!always && told != null && Math.abs(told[0] - snow) < 0.002f
                && Math.abs(told[1] - sand.amount()) < 0.002f && told[2] == red) return;
        coverTold.put(player, new float[]{snow, sand.amount(), red});
        PacketDistributor.sendToPlayer(player, new GroundCoverSyncPacket(snow, sand.amount(), sand.red()));
    }

    private void syncPacketToDimension(ServerLevel level, CustomPacketPayload packet) {
        PacketDistributor.sendToPlayersInDimension(level, packet);
    }

    // NBT SAVE & LOAD
    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider provider) {
        for (WeatherTypes.Dimension dim : WeatherTypes.Dimension.values()) {
            CompoundTag dimTag = new CompoundTag();
            dimTag.putString("weatherType", currentWeathers.get(dim).name());
            dimTag.putInt("weatherTicksRemaining", weatherTimers.get(dim));
            dimTag.putInt("realWeatherTimer", realWeatherTickTimers.get(dim));
            dimTag.putLong("lastGameTick", lastGameTicks.get(dim));
            winds.get(dim).save(dimTag);

            atmospheres.get(dim).save(dimTag);
            histories.get(dim).save(dimTag);
            durationTrackers.get(dim).save(dimTag);
            snowPacks.get(dim).save(dimTag);
            sandDrifts.get(dim).save(dimTag);

            tag.put("dim_" + dim.name(), dimTag);
        }
        tag.putLong("rainbowStart", rainbowStart);
        tag.putLong("rainbowEnd", rainbowEnd);
        return tag;
    }

    private static WeatherManager load(CompoundTag tag, HolderLookup.Provider provider) {
        WeatherManager m = new WeatherManager();
        for (WeatherTypes.Dimension dim : WeatherTypes.Dimension.values()) {
            String key = "dim_" + dim.name();
            if (tag.contains(key)) {
                CompoundTag dimTag = tag.getCompound(key);
                m.currentWeathers.put(dim, WeatherTypes.fromNameSafe(dimTag.getString("weatherType")));
                m.weatherTimers.put(dim, dimTag.getInt("weatherTicksRemaining"));
                m.realWeatherTickTimers.put(dim, dimTag.getInt("realWeatherTimer"));
                m.lastGameTicks.put(dim, dimTag.getLong("lastGameTick"));
                m.winds.get(dim).load(dimTag);

                m.atmospheres.get(dim).load(dimTag);
                m.histories.get(dim).load(dimTag);
                m.durationTrackers.get(dim).load(dimTag);
                m.snowPacks.get(dim).load(dimTag);
                m.sandDrifts.get(dim).load(dimTag);
            }
        }
        m.rainbowStart = tag.getLong("rainbowStart");
        m.rainbowEnd = tag.getLong("rainbowEnd");
        return m;
    }

    private static final Factory<WeatherManager> FACTORY =
            new Factory<>(WeatherManager::new, WeatherManager::load);

    public static WeatherManager get(ServerLevel level) {
        WeatherManager manager = level.getDataStorage().computeIfAbsent(FACTORY, NAME);

        WeatherTypes.Dimension dim = DimensionProfile.getDimension(level);
        WeatherTypes current = manager.currentWeathers.get(dim);

        if (!isCompatibleWithLevel(current, level)) {
            WeatherTypes fallback = defaultForDimension(dim);
            manager.currentWeathers.put(dim, fallback);
            manager.setDirty();
        }

        if (manager.weatherTimers.get(dim) < 0) {
            manager.startAutoWeather(level);
        }

        return manager;
    }
}