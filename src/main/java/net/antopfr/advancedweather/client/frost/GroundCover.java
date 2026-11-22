package net.antopfr.advancedweather.client.frost;

import net.antopfr.advancedweather.AdvancedWeather;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

@EventBusSubscriber(modid = AdvancedWeather.MOD_ID, value = Dist.CLIENT)
public final class GroundCover {

    private GroundCover() {}

    public static final class Glide {
        private float base;
        private float pace;
        private float to;

        public float base() {
            return base;
        }

        public float pace() {
            return pace;
        }

        private float at(float clock) {
            return Mth.clamp(base + pace * clock, 0f, 1f);
        }

        private void hold(float value) {
            base = to = value;
            pace = 0f;
        }

        private void aim(float from, float target, int start, int length) {
            to = target;
            pace = (target - from) / length;
            base = from - pace * start;
        }
    }

    private static final Glide STILL = new Glide();

    private static final int HERE_GLIDE = 20;
    private static final int HERE_GLIDE_LONGEST = 100;
    private static final int TABLE_GLIDE = 100;
    private static final int TABLE_GLIDE_SHORTEST = 20;
    private static final int TABLE_GLIDE_LONGEST = 300;

    private static ClientLevel trackedLevel;
    private static int ticks;

    private static final Glide snow = new Glide();
    private static final Glide sand = new Glide();
    private static boolean redSand;
    private static boolean settled;
    private static int hereHeard;
    private static int hereEnd;

    private static final Map<ResourceLocation, Glide> table = new HashMap<>();
    private static boolean tableKnown;
    private static int tableHeard;
    private static int tableEnd;

    private static final Set<ResourceLocation> nearbyBiomes = new HashSet<>();
    private static int nearbyCountdown;

    private static final int NEARBY_REACH = 128;
    private static final int NEARBY_STEP = 16;
    private static final int NEARBY_TICKS = 20;

    private static float hereClock(float partialTick) {
        return Math.min(ticks + partialTick, hereEnd);
    }

    public static float snowClock(float partialTick) {
        return Math.min(ticks + partialTick, tableEnd);
    }

    public static float snow(float partialTick) {
        return snow.at(hereClock(partialTick));
    }

    public static float sand(float partialTick) {
        return sand.at(hereClock(partialTick));
    }

    public static boolean redSand() {
        return redSand;
    }

    public static boolean columnsKnown() {
        return tableKnown;
    }

    public static Glide glideOf(Holder<Biome> biome) {
        ResourceKey<Biome> key = biome.unwrapKey().orElse(null);
        if (key == null) return STILL;
        Glide glide = table.get(key.location());
        return glide == null ? STILL : glide;
    }

    public static float snowNearby(float partialTick) {
        float clock = snowClock(partialTick);
        float most = snow(partialTick);
        for (ResourceLocation biome : nearbyBiomes) {
            Glide glide = table.get(biome);
            if (glide != null) most = Math.max(most, glide.at(clock));
        }
        return most;
    }

    private static int glideLength(int heard, int usual, int shortest, int longest) {
        if (heard < shortest) return shortest;
        return heard > longest ? usual : heard;
    }

    public static void syncTable(Map<ResourceLocation, Float> told) {
        int length = glideLength(ticks - tableHeard, TABLE_GLIDE, TABLE_GLIDE_SHORTEST, TABLE_GLIDE_LONGEST);
        float now = Math.min(ticks, tableEnd);
        for (ResourceLocation biome : told.keySet()) table.computeIfAbsent(biome, key -> new Glide());
        Iterator<Map.Entry<ResourceLocation, Glide>> lying = table.entrySet().iterator();
        while (lying.hasNext()) {
            Map.Entry<ResourceLocation, Glide> entry = lying.next();
            Glide glide = entry.getValue();
            float from = glide.at(now);
            float target = Mth.clamp(told.getOrDefault(entry.getKey(), 0f), 0f, 1f);
            if (from <= 0f && target <= 0f) {
                lying.remove();
            } else if (tableKnown) {
                glide.aim(from, target, ticks, length);
            } else {
                glide.hold(target);
            }
        }
        tableEnd = tableKnown ? ticks + length : ticks;
        tableHeard = ticks;
        tableKnown = true;
    }

    public static void sync(float snowLying, float sandLying, boolean sandIsRed) {
        float snowTarget = Mth.clamp(snowLying, 0f, 1f);
        float sandTarget = Mth.clamp(sandLying, 0f, 1f);
        if (sandTarget > 0f) redSand = sandIsRed;
        if (settled) {
            int length = glideLength(ticks - hereHeard, HERE_GLIDE, HERE_GLIDE, HERE_GLIDE_LONGEST);
            float now = Math.min(ticks, hereEnd);
            snow.aim(snow.at(now), snowTarget, ticks, length);
            sand.aim(sand.at(now), sandTarget, ticks, length);
            hereEnd = ticks + length;
        } else {
            settled = true;
            snow.hold(snowTarget);
            sand.hold(sandTarget);
            hereEnd = ticks;
        }
        hereHeard = ticks;
    }

    private static void restartClock() {
        ticks = 0;
        hereHeard = hereEnd = 0;
        tableHeard = tableEnd = 0;
        snow.hold(snow.to);
        sand.hold(sand.to);
        for (Glide glide : table.values()) glide.hold(glide.to);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            trackedLevel = null;
            snow.hold(0f);
            sand.hold(0f);
            settled = false;
            table.clear();
            tableKnown = false;
            nearbyBiomes.clear();
            restartClock();
            return;
        }
        if (mc.isPaused()) return;

        if (level != trackedLevel) {
            trackedLevel = level;
            restartClock();
            settled = false;
            nearbyBiomes.clear();
            nearbyCountdown = 0;
            return;
        }

        ticks++;

        if (--nearbyCountdown <= 0) {
            nearbyCountdown = NEARBY_TICKS;
            scanNearby(level, mc.gameRenderer.getMainCamera().getBlockPosition());
        }
    }

    private static void scanNearby(ClientLevel level, BlockPos centre) {
        nearbyBiomes.clear();
        if (!tableKnown) return;
        for (int dz = -NEARBY_REACH; dz <= NEARBY_REACH; dz += NEARBY_STEP) {
            for (int dx = -NEARBY_REACH; dx <= NEARBY_REACH; dx += NEARBY_STEP) {
                int x = centre.getX() + dx;
                int z = centre.getZ() + dz;
                LevelChunk chunk = level.getChunkSource().getChunkNow(x >> 4, z >> 4);
                if (chunk == null) continue;
                int top = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
                chunk.getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(top), QuartPos.fromBlock(z))
                        .unwrapKey().ifPresent(key -> nearbyBiomes.add(key.location()));
            }
        }
    }
}
