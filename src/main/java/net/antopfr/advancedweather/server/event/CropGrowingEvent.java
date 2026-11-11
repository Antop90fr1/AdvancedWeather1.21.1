package net.antopfr.advancedweather.server.event;

import net.antopfr.advancedweather.config.AWCommonConfig;
import net.antopfr.advancedweather.weather.ServerRain;
import net.antopfr.advancedweather.weather.WeatherDurationTracker;
import net.antopfr.advancedweather.weather.WeatherManager;
import net.antopfr.advancedweather.weather.WeatherTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.block.CropGrowEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

@EventBusSubscriber(modid = "advancedweather")
public class CropGrowingEvent {

    private static final int SCAN_RADIUS = 8;
    private static final int SCAN_INTERVAL = 600; // ticks (30s)

    private static final float DRIZZLE_GROWTH = 0.15f;
    private static final float RAIN_GROWTH = 0.3f;
    private static final float SUNNY_GROWTH = 0.12f;
    private static final float CLEAR_GROWTH = 0.06f;
    private static final float SAPLING_TURN = 1f / 7f;
    private static final float ICE_KILL = 0.3f;

    @SubscribeEvent
    public static void onCropGrow(CropGrowEvent.Pre event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        WeatherManager manager = WeatherManager.get(level);
        WeatherDurationTracker tracker = manager.getDurationTracker(level);
        WeatherTypes type = manager.getCurrentWeather(level);

        if (type == WeatherTypes.HEAVY_RAIN && tracker.hasLastedMinutes(WeatherTypes.HEAVY_RAIN, 10)
                || type == WeatherTypes.HAIL
                || type == WeatherTypes.BLIZZARD
                || type == WeatherTypes.FREEZING_RAIN) {
            event.setResult(CropGrowEvent.Pre.Result.DO_NOT_GROW);
            return;
        }

        if (level.random.nextFloat() < extraGrowth(level, type, event.getPos())) {
            event.setResult(CropGrowEvent.Pre.Result.GROW);
        }
    }

    private static float extraGrowth(ServerLevel level, WeatherTypes type, BlockPos pos) {
        if (type == null) return 0f;
        AWCommonConfig config = AWCommonConfig.get();
        float extra = switch (type) {
            case DRIZZLE -> config.rainBoostsCrops ? DRIZZLE_GROWTH : 0f;
            case LIGHT_RAIN -> config.rainBoostsCrops ? RAIN_GROWTH : 0f;
            case SUNNY -> config.sunBoostsCrops && level.isDay() ? SUNNY_GROWTH : 0f;
            case CLEAR -> config.sunBoostsCrops && level.isDay() ? CLEAR_GROWTH : 0f;
            default -> 0f;
        };
        return extra > 0f && level.canSeeSky(pos.above()) ? extra : 0f;
    }

    public static float saplingGrowth(ServerLevel level, BlockPos pos) {
        WeatherTypes type = ServerRain.weatherOf(level);
        if (type != WeatherTypes.DRIZZLE && type != WeatherTypes.LIGHT_RAIN) return 0f;
        return extraGrowth(level, type, pos) / RAIN_GROWTH * SAPLING_TURN;
    }

    public static boolean killedByIce(ServerLevel level, BlockPos pos, RandomSource random) {
        if (!AWCommonConfig.get().frostKillsCrops || ServerRain.weatherOf(level) != WeatherTypes.FREEZING_RAIN) return false;
        if (!WeatherManager.get(level).getDurationTracker(level).hasLastedMinutes(WeatherTypes.FREEZING_RAIN, 2)) return false;
        return level.canSeeSky(pos.above()) && random.nextFloat() < ICE_KILL;
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!level.dimension().equals(Level.OVERWORLD)) return;
        if (level.getGameTime() % SCAN_INTERVAL != 0) return;

        AWCommonConfig config = AWCommonConfig.get();
        WeatherManager manager = WeatherManager.get(level);
        WeatherDurationTracker tracker = manager.getDurationTracker(level);
        WeatherTypes type = manager.getCurrentWeather(level);
        float temperature = manager.getAtmosphere(level).getTemperature();

        boolean regress = config.heavyRainSlowCrops && type == WeatherTypes.HEAVY_RAIN
                && tracker.hasLastedMinutes(WeatherTypes.HEAVY_RAIN, 15);
        boolean frost = config.frostKillsCrops
                && type == WeatherTypes.BLIZZARD
                && tracker.hasLastedMinutes(type, 2);
        boolean drought = config.droughtDriesFarmland
                && (type == WeatherTypes.CLEAR || type == WeatherTypes.SUNNY)
                && tracker.hasLastedMinutes(type, 20) && temperature > 25f;

        if (!regress && !frost && !drought) return;

        for (ServerPlayer player : level.players()) {
            BlockPos center = player.blockPosition();
            for (int dx = -SCAN_RADIUS; dx <= SCAN_RADIUS; dx++) {
                for (int dz = -SCAN_RADIUS; dz <= SCAN_RADIUS; dz++) {
                    int x = center.getX() + dx;
                    int z = center.getZ() + dz;
                    int topY = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
                    BlockPos pos = new BlockPos(x, topY, z);
                    if (!level.canSeeSky(pos)) continue; // only crops open to the sky are affected

                    BlockState state = level.getBlockState(pos);
                    if (state.getBlock() instanceof CropBlock crop) {
                        applyToCrop(level, pos, state, crop, frost, regress);
                        if (drought) dryFarmland(level, pos.below());
                    } else if (drought && state.getBlock() instanceof FarmBlock) {
                        dryFarmland(level, pos);
                    }
                }
            }
        }
    }

    private static void applyToCrop(ServerLevel level, BlockPos pos, BlockState state, CropBlock crop,
                                    boolean frost, boolean regress) {
        if (frost) {
            if (level.random.nextFloat() < 0.15f) level.destroyBlock(pos, false);
            return;
        }
        // Skip crops that don't use the standard age property (e.g. beetroot) rather than crash.
        if (!state.hasProperty(CropBlock.AGE)) return;
        int age = state.getValue(CropBlock.AGE);
        if (regress && age > 0 && level.random.nextFloat() < 0.3f) {
            level.setBlock(pos, crop.getStateForAge(age - 1), 2);
        }
    }

    private static void dryFarmland(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof FarmBlock && state.getValue(FarmBlock.MOISTURE) > 0) {
            level.setBlock(pos, state.setValue(FarmBlock.MOISTURE, 0), 2);
        }
    }
}
