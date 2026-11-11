package net.antopfr.advancedweather.weather.effect.types.rainbows;

import net.antopfr.advancedweather.AdvancedWeather;
import net.antopfr.advancedweather.api.event.WeatherChangeEvent;
import net.antopfr.advancedweather.content.advancement.AWAdvancements;
import net.antopfr.advancedweather.weather.WeatherDefinitions;
import net.antopfr.advancedweather.weather.WeatherEffects;
import net.antopfr.advancedweather.weather.WeatherManager;
import net.antopfr.advancedweather.weather.WeatherTypes;
import net.antopfr.advancedweather.weather.effect.EffectManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.EnumSet;
import java.util.Set;

@EventBusSubscriber(modid = AdvancedWeather.MOD_ID)
public final class RainbowSpawner {

    private RainbowSpawner() {}

    private static final Set<WeatherTypes> RAIN_TYPES = EnumSet.of(
            WeatherTypes.LIGHT_RAIN, WeatherTypes.HEAVY_RAIN, WeatherTypes.DRIZZLE);

    private static final int MIN_LIFETIME = 3600;
    private static final int MAX_LIFETIME = 7200;
    private static final int SHOWER_CHECK_INTERVAL = 1200;
    private static final float SHOWER_CHANCE = 0.25f;

    @SubscribeEvent
    public static void onWeatherChange(WeatherChangeEvent event) {
        ServerLevel level = event.getLevel();
        if (!level.dimension().equals(Level.OVERWORLD)) return;

        if (WeatherDefinitions.get(event.getCurrent()).hidesSun) {
            clear(level);
        } else if (RAIN_TYPES.contains(event.getPrevious()) && !event.getCurrent().isVanillaRaining()) {
            trySpawn(level);
        }
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !level.dimension().equals(Level.OVERWORLD)) return;
        long gameTime = level.getGameTime();
        if (gameTime % 20 != 0) return;

        WeatherManager manager = WeatherManager.get(level);
        if (isActive(manager, gameTime)) {
            creditWitnesses(level, manager, gameTime);
            return;
        }

        if (gameTime % SHOWER_CHECK_INTERVAL != 0) return;
        WeatherTypes current = manager.getCurrentWeather(level);
        if (RAIN_TYPES.contains(current) && !WeatherDefinitions.get(current).hidesSun
                && level.getRandom().nextFloat() < SHOWER_CHANCE) {
            trySpawn(level);
        }
    }

    public static boolean trySpawn(ServerLevel level) {
        WeatherManager manager = WeatherManager.get(level);
        if (isActive(manager, level.getGameTime())) return false;
        if (!EffectManager.get(level).hasEffect(WeatherEffects.RAINBOWS)) return false;

        int lifetime = randomLifetime(level);
        if (!sunWillSuit(level, lifetime)) return false;
        start(level, lifetime);
        return true;
    }

    public static boolean spawn(ServerLevel level) {
        start(level, randomLifetime(level));
        return Rainbow.sunFactor(Rainbow.sunElevationDeg(level.getTimeOfDay(1f))) > 0.5f;
    }

    public static boolean clear(ServerLevel level) {
        WeatherManager manager = WeatherManager.get(level);
        long now = level.getGameTime();
        if (!isActive(manager, now)) return false;
        manager.setRainbow(level, manager.getRainbowStart(), Math.min(manager.getRainbowEnd(), now + Rainbow.FADE_OUT_TICKS));
        return true;
    }

    private static void start(ServerLevel level, int lifetime) {
        long now = level.getGameTime();
        WeatherManager.get(level).setRainbow(level, now, now + lifetime);
    }

    private static int randomLifetime(ServerLevel level) {
        return MIN_LIFETIME + level.getRandom().nextInt(MAX_LIFETIME - MIN_LIFETIME);
    }

    private static boolean isActive(WeatherManager manager, long gameTime) {
        return manager.getRainbowStart() < manager.getRainbowEnd() && gameTime < manager.getRainbowEnd();
    }

    private static boolean sunWillSuit(ServerLevel level, int lifetime) {
        long dayTime = level.getDayTime();
        for (int step = 0; step <= 4; step++) {
            float timeOfDay = level.dimensionType().timeOfDay(dayTime + (long) lifetime * step / 4);
            if (Rainbow.sunFactor(Rainbow.sunElevationDeg(timeOfDay)) > 0.5f) return true;
        }
        return false;
    }

    private static void creditWitnesses(ServerLevel level, WeatherManager manager, long gameTime) {
        if (WeatherDefinitions.get(manager.getCurrentWeather(level)).hidesSun) return;

        float timeOfDay = level.getTimeOfDay(1f);
        float shown = Rainbow.envelope(gameTime, manager.getRainbowStart(), manager.getRainbowEnd())
                * Rainbow.sunFactor(Rainbow.sunElevationDeg(timeOfDay));
        if (shown < 0.5f) return;

        double towardsBow = Math.signum(Mth.sin(timeOfDay * Mth.TWO_PI));
        for (ServerPlayer player : level.players()) {
            if (player.getLookAngle().x * towardsBow < 0.3) continue;
            if (!level.canSeeSky(BlockPos.containing(player.getEyePosition()))) continue;
            AWAdvancements.grant(player, "somewhere_over");
        }
    }
}
