package net.antopfr.advancedweather.compat.sereneseasons;

import net.antopfr.advancedweather.compat.eclipticseasons.EclipticSeasonsCompat;
import net.antopfr.advancedweather.util.ModLoaded;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SeasonCompat {

    private static final int REFRESH_TICKS = 20;

    private record Reading(long at, float progress) {}

    private static final Map<ResourceKey<Level>, Reading> READINGS = new ConcurrentHashMap<>();

    public static float yearProgress(ServerLevel level) {
        if (!ModLoaded.isEclipticSeasonsLoaded() && !ModLoaded.isSereneSeasonsLoaded()) return -1f;

        long now = level.getGameTime();
        Reading last = READINGS.get(level.dimension());
        if (last != null && now >= last.at() && now - last.at() < REFRESH_TICKS) return last.progress();

        float progress = read(level);
        READINGS.put(level.dimension(), new Reading(now, progress));
        return progress;
    }

    private static float read(ServerLevel level) {
        try {
            if (ModLoaded.isEclipticSeasonsLoaded()) return EclipticSeasonsCompat.yearProgress(level);
            return SeasonCompatImpl.yearProgress(level);
        } catch (Throwable t) {
            return -1f;
        }
    }

    public static int getCurrentSeasonOrdinal(ServerLevel level) {
        float progress = yearProgress(level);
        return progress < 0f ? -1 : Math.min(3, (int) (progress * 4f));
    }
}
