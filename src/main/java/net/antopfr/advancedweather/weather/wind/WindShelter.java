package net.antopfr.advancedweather.weather.wind;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

public final class WindShelter {

    private WindShelter() {}

    private static final int SIDE_REACH = 6;

    public static float exposure(Level level, BlockPos pos) {
        int roofY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ());
        if (roofY <= pos.getY() + 1) return 1f;

        int distanceToRoof = roofY - pos.getY();
        float vertical = 1f - (float) Math.exp(-distanceToRoof * 0.15f);

        BlockState roof = level.getBlockState(new BlockPos(pos.getX(), roofY - 1, pos.getZ()));
        float material = roofTransmission(roof.getSoundType());

        int open = 0;
        for (int[] dir : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
            boolean blocked = false;
            for (int dist = 1; dist <= SIDE_REACH; dist++) {
                if (!level.isEmptyBlock(pos.offset(dir[0] * dist, 0, dir[1] * dist))) {
                    blocked = true;
                    break;
                }
            }
            if (!blocked) open++;
        }
        float openness = 0.1f + (open / 4f) * 0.7f;

        float shelter = vertical * material * openness;
        return Mth.clamp(0.05f + shelter * 0.95f, 0.05f, 1f);
    }

private static float roofTransmission(SoundType sound) {
        if (sound == SoundType.GLASS) return 0.85f;
        if (sound == SoundType.METAL || sound == SoundType.COPPER) return 0.6f;
        if (sound == SoundType.WOOD || sound == SoundType.CHERRY_WOOD) return 0.4f;
        if (sound == SoundType.STONE || sound == SoundType.DEEPSLATE) return 0.3f;
        return 0.35f;
    }
}
