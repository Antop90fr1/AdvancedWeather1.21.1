package net.antopfr.advancedweather.compat.eclipticseasons;

import net.antopfr.advancedweather.config.AWClientConfig;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class EclipticSnowChoice {

    private EclipticSnowChoice() {}

    public static boolean ours(BlockState state) {
        AWClientConfig config = AWClientConfig.get();
        if (!config.snowCover) return false;
        boolean ours = leaves(state) ? config.snowyLeaves : !config.eclipticBlockSnow;
        if (ours) EclipticSeasonsCompat.report(64, "snow choice");
        return ours;
    }

    private static boolean leaves(BlockState state) {
        return state.getBlock() instanceof LeavesBlock || state.is(BlockTags.LEAVES);
    }
}
