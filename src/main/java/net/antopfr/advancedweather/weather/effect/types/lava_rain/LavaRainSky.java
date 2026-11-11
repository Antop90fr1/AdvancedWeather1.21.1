package net.antopfr.advancedweather.weather.effect.types.lava_rain;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

public final class LavaRainSky {

    private LavaRainSky() {}

    private static final int FIRST_OPENING = 8;
    private static final int TALL_OPENING = 16;

    public record Opening(int floor, int roof) {}

    public static Opening below(Level level, int x, double y, int z) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int bottom = level.getMinBuildHeight();
        int top = bottom + level.dimensionType().logicalHeight() - 1;

        boolean first = true;
        int clear = 0;
        for (int at = top; at >= bottom; at--) {
            if (!Heightmap.Types.MOTION_BLOCKING.isOpaque().test(level.getBlockState(cursor.set(x, at, z)))) {
                clear++;
                continue;
            }
            if (clear >= FIRST_OPENING) {
                if ((first || clear >= TALL_OPENING) && at + 1 < y) return new Opening(at, at + clear + 1);
                first = false;
            }
            clear = 0;
        }
        return null;
    }
}
