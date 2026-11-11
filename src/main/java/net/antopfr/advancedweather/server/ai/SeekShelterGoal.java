package net.antopfr.advancedweather.server.ai;

import net.antopfr.advancedweather.server.event.SandstormEffect;
import net.antopfr.advancedweather.weather.WeatherManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathfindingContext;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.EnumSet;
import java.util.List;

public class SeekShelterGoal extends Goal {

    private static final int SEARCH_RADIUS = 20;
    private static final int SEARCH_HEIGHT = 5;
    private static final int SHUFFLE_RADIUS = 4;
    private static final int AREA_SIZE = 6;
    private static final int PATHS_PER_SEARCH = 3;
    private static final int PATH_RANGE = 32;
    private static final double RUN_SPEED = 1.4;
    private static final double SHUFFLE_SPEED = 0.8;

    private final PathfinderMob mob;
    private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

    private Path path;
    private boolean threatened;
    private boolean settled;
    private int checkDelay;
    private int weatherDelay;
    private int failedSearches;

    public SeekShelterGoal(PathfinderMob mob) {
        this.mob = mob;
        this.checkDelay = mob.getRandom().nextInt(reducedTickDelay(40));
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (checkDelay-- > 0) return false;
        checkDelay = reducedTickDelay(30 + mob.getRandom().nextInt(20));

        if (!canMove() || !isThreatened()) return false;
        if (isCovered(mob.blockPosition())) {
            path = null;
            return true;
        }
        if (!mob.onGround() && !mob.isInLiquid()) {
            checkDelay = 2;
            return false;
        }

        path = pathToShelter();
        if (path == null) {
            checkDelay = reducedTickDelay(100 + 40 * Math.min(failedSearches, 5) + mob.getRandom().nextInt(60));
            return false;
        }
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (!threatened || !canMove()) return false;
        return !mob.getNavigation().isDone() || isCovered(mob.blockPosition());
    }

    @Override
    public void start() {
        threatened = true;
        settled = path == null;
        weatherDelay = adjustedTickDelay(40);
        if (path != null) mob.getNavigation().moveTo(path, RUN_SPEED);
    }

    @Override
    public void tick() {
        if (--weatherDelay <= 0) {
            weatherDelay = adjustedTickDelay(40);
            threatened = isThreatened();
        }
        if (!mob.getNavigation().isDone()) return;

        settled = true;
        if (mob.getRandom().nextInt(adjustedTickDelay(240)) == 0) {
            Path shuffle = pathToAnotherSpot();
            if (shuffle != null) mob.getNavigation().moveTo(shuffle, SHUFFLE_SPEED);
        }
    }

    @Override
    public void stop() {
        boolean stranded = threatened && !settled
                && mob.getNavigation().isDone() && !isCovered(mob.blockPosition());
        if (stranded) failedSearches++;
        checkDelay = reducedTickDelay(stranded ? 100 + mob.getRandom().nextInt(60) : 20);

        path = null;
        mob.getNavigation().stop();
    }

    private boolean canMove() {
        return !mob.isLeashed() && !mob.isVehicle() && !mob.isPassenger();
    }

    private boolean isThreatened() {
        if (!(mob.level() instanceof ServerLevel level) || !level.dimension().equals(Level.OVERWORLD)) return false;
        return switch (WeatherManager.get(level).getCurrentWeather(level)) {
            case HAIL, BLIZZARD, THUNDERSTORM -> true;
            case SANDSTORM -> SandstormEffect.isSandyBiome(level, mob.blockPosition());
            default -> false;
        };
    }

    private boolean isCovered(BlockPos pos) {
        return isCovered(mob.level(), pos.getX(), pos.getY(), pos.getZ());
    }

    private static boolean isCovered(Level level, int x, int y, int z) {
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) > y + 1;
    }

    private Path pathToShelter() {
        List<BlockPos> spots = findSpots(SEARCH_RADIUS, (failedSearches + 1) * PATHS_PER_SEARCH, true);

        int from = failedSearches * PATHS_PER_SEARCH;
        if (from >= spots.size()) {
            failedSearches = 0;
            from = 0;
        }
        for (int i = from; i < Math.min(from + PATHS_PER_SEARCH, spots.size()); i++) {
            Path candidate = pathTo(spots.get(i));
            if (candidate != null) {
                failedSearches = 0;
                return candidate;
            }
        }
        failedSearches++;
        return null;
    }

    private Path pathToAnotherSpot() {
        List<BlockPos> spots = findSpots(SHUFFLE_RADIUS, Integer.MAX_VALUE, false);
        return spots.isEmpty() ? null : pathTo(spots.get(mob.getRandom().nextInt(spots.size())));
    }

    private Path pathTo(BlockPos spot) {
        int corner = (int) (mob.getBbWidth() + 1f) / 2;
        Path candidate = mob.getNavigation().createPath(spot.offset(-corner, 0, -corner), 1, PATH_RANGE);
        return candidate != null && candidate.canReach() ? candidate : null;
    }

    private List<BlockPos> findSpots(int radius, int wanted, boolean onePerArea) {
        ServerLevel level = (ServerLevel) mob.level();
        PathfindingContext context = new PathfindingContext(level, mob);
        BlockPos origin = mob.blockPosition();
        int headroom = Mth.ceil(mob.getBbHeight());

        int areasPerSide = 2 * (radius / AREA_SIZE + 1);
        BitSet takenAreas = onePerArea ? new BitSet(areasPerSide * areasPerSide) : null;
        List<BlockPos> spots = new ArrayList<>();

        for (int ring = 1; ring <= radius && spots.size() < wanted; ring++) {
            for (int dx = -ring; dx <= ring; dx++) {
                int step = Math.abs(dx) == ring ? 1 : 2 * ring;
                for (int dz = -ring; dz <= ring; dz += step) {
                    int area = -1;
                    if (takenAreas != null) {
                        area = (Math.floorDiv(dx, AREA_SIZE) + areasPerSide / 2) * areasPerSide
                                + Math.floorDiv(dz, AREA_SIZE) + areasPerSide / 2;
                        if (takenAreas.get(area)) continue;
                    }

                    BlockPos spot = spotIn(level, context, origin.getX() + dx, origin.getZ() + dz, origin.getY(), headroom);
                    if (spot == null) continue;
                    if (takenAreas != null) takenAreas.set(area);
                    spots.add(spot);
                }
            }
        }
        return spots;
    }

    private BlockPos spotIn(ServerLevel level, PathfindingContext context, int x, int z, int originY, int headroom) {
        int roof = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        if (roof <= originY - SEARCH_HEIGHT + 1) return null;

        for (int step = 0; step <= 2 * SEARCH_HEIGHT; step++) {
            int y = originY + ((step & 1) == 0 ? step / 2 : -(step + 1) / 2);
            if (roof <= y + 1) continue;
            if (WalkNodeEvaluator.getPathTypeStatic(context, cursor.set(x, y, z)) != PathType.WALKABLE) continue;
            if (!hasHeadroom(level, x, y, z, headroom) || !isCoveredAround(level, x, y, z)) continue;
            if (!mob.isWithinRestriction(cursor.set(x, y, z))) continue;
            return cursor.immutable();
        }
        return null;
    }

    private boolean hasHeadroom(ServerLevel level, int x, int y, int z, int headroom) {
        for (int up = 1; up < headroom; up++) {
            cursor.set(x, y + up, z);
            if (!level.getBlockState(cursor).getCollisionShape(level, cursor).isEmpty()) return false;
        }
        return true;
    }

    private static boolean isCoveredAround(ServerLevel level, int x, int y, int z) {
        return isCovered(level, x - 1, y, z) && isCovered(level, x + 1, y, z)
                && isCovered(level, x, y, z - 1) && isCovered(level, x, y, z + 1);
    }
}
