package net.antopfr.advancedweather.content.block.sensor;

import net.antopfr.advancedweather.compat.catnip.CatnipCompat;
import net.minecraft.core.BlockPos;

import java.util.List;

public class SensorHighlighter {
    private static final int DURATION_TICKS = 100;

    public static void show(List<BlockPos> valid, List<BlockPos> invalid) {
        CatnipCompat.showSensorOutlines(valid, invalid, DURATION_TICKS);
    }
}