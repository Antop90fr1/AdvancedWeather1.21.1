package net.antopfr.advancedweather.compat.catnip;

import net.antopfr.advancedweather.util.ModLoaded;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import java.util.List;

public final class CatnipCompat {

    private CatnipCompat() {}

    public static Smoothed smoothed() {
        return ModLoaded.isCreateLoaded() ? new CatnipSmoothed() : new PlainSmoothed();
    }

    public static long animationTicks() {
        if (ModLoaded.isCreateLoaded()) {
            return CatnipBridge.animationTicks();
        }
        var level = Minecraft.getInstance().level;
        return level != null ? level.getGameTime() : 0L;
    }

    public static void showSensorOutlines(List<BlockPos> valid, List<BlockPos> invalid, int durationTicks) {
        if (ModLoaded.isCreateLoaded()) {
            CatnipBridge.showSensorOutlines(valid, invalid, durationTicks);
        } else {
            PlainOutliner.show(valid, invalid, durationTicks);
        }
    }
}
