package net.antopfr.advancedweather.compat.catnip;

import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

import java.util.List;

final class CatnipBridge {

    private CatnipBridge() {}

    static long animationTicks() {
        return AnimationTickHolder.getTicks();
    }

    static void showSensorOutlines(List<BlockPos> valid, List<BlockPos> invalid, int durationTicks) {
        int i = 0;
        for (BlockPos pos : valid) {
            Outliner.getInstance().showAABB("aw_sensor_" + (i++), new AABB(pos), durationTicks)
                    .colored(0x4AE05A) // green
                    .lineWidth(1 / 16f);
        }
        for (BlockPos pos : invalid) {
            Outliner.getInstance().showAABB("aw_sensor_" + (i++), new AABB(pos), durationTicks)
                    .colored(0xE0A030) // amber
                    .lineWidth(1 / 16f);
        }
    }
}
