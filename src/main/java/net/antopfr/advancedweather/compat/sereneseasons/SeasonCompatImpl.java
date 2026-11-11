package net.antopfr.advancedweather.compat.sereneseasons;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import sereneseasons.api.season.ISeasonState;
import sereneseasons.api.season.SeasonHelper;
import sereneseasons.init.ModConfig;

class SeasonCompatImpl {

    static float yearProgress(ServerLevel level) {
        if (!ModConfig.seasons.isDimensionWhitelisted(level.dimension())) return -1f;

        ISeasonState state = SeasonHelper.getSeasonState(level);
        int year = state.getCycleDuration();
        if (year <= 0) return -1f;
        return Mth.clamp((float) state.getSeasonCycleTicks() / year, 0f, 0.9999f);
    }
}
