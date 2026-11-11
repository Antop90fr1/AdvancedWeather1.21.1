package net.antopfr.advancedweather.weather;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

import java.util.HashMap;
import java.util.Map;

public final class SandDrift {

    private static final float BUILD_TICKS = 20 * 60 * 3;

    private static final float SWEEP_TICKS = 20 * 60 * 4;
    private static final float STILL = 0.15f;

    private static final float WASH = 3f;


    private static final int REACH = 64;
    private static final int RING = 8;

    private static final int[][] AROUND = {
            {1000, 0}, {707, 707}, {0, 1000}, {-707, 707}, {-1000, 0}, {-707, -707}, {0, -1000}, {707, -707}
    };

    public record Lying(float amount, boolean red) {
        private static final Lying NONE = new Lying(0f, false);
    }

    private final Map<ResourceLocation, Float> sand = new HashMap<>();

    public static boolean isSandy(Holder<Biome> biome) {
        return biome.is(Biomes.DESERT) || biome.is(Biomes.BEACH) || biome.is(BiomeTags.IS_BADLANDS);
    }

    public float sand(ResourceLocation biome) {
        if (biome == null) return 0f;
        Float lying = sand.get(biome);
        return lying == null ? 0f : lying;
    }

    public Lying lyingAt(ServerLevel level, BlockPos pos) {
        if (sand.isEmpty()) return Lying.NONE;

        Holder<Biome> here = level.getBiome(pos);
        float most = sand(here.unwrapKey().map(ResourceKey::location).orElse(null));
        boolean red = most > 0f && here.is(BiomeTags.IS_BADLANDS);

        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int distance = RING; distance < REACH; distance += RING) {
            float share = 1f - distance / (float) REACH;
            if (share <= most) break;

            for (int[] way : AROUND) {
                at.set(pos.getX() + way[0] * distance / 1000, pos.getY(), pos.getZ() + way[1] * distance / 1000);
                Holder<Biome> there = level.getBiome(at);
                float lying = sand(there.unwrapKey().map(ResourceKey::location).orElse(null)) * share;
                if (lying > most) {
                    most = lying;
                    red = there.is(BiomeTags.IS_BADLANDS);
                }
            }
        }
        return most > 0f ? new Lying(most, red) : Lying.NONE;
    }

    public void tick(ServerLevel level, WeatherTypes weather, float wind) {
        if (weather == WeatherTypes.SANDSTORM) {
            float laid = SnowPack.STEP / BUILD_TICKS;
            level.registryAccess().registryOrThrow(Registries.BIOME).holders().forEach(biome -> {
                if (isSandy(biome)) {
                    ResourceLocation id = biome.key().location();
                    sand.put(id, Math.min(1f, sand(id) + laid));
                }
            });
            return;
        }
        if (sand.isEmpty()) return;

        float swept = SnowPack.STEP * (STILL + wind) / SWEEP_TICKS;
        if (WeatherDefinitions.get(weather).rainIntensity > 0f) swept *= WASH;

        float gone = swept;
        sand.replaceAll((biome, lying) -> lying - gone);
        sand.values().removeIf(lying -> lying <= 0f);
    }

    public void save(CompoundTag tag) {
        CompoundTag lying = new CompoundTag();
        sand.forEach((biome, amount) -> lying.putFloat(biome.toString(), amount));
        tag.put("sandDrift", lying);
    }

    public void load(CompoundTag tag) {
        sand.clear();
        CompoundTag lying = tag.getCompound("sandDrift");
        for (String key : lying.getAllKeys()) {
            ResourceLocation biome = ResourceLocation.tryParse(key);
            float amount = lying.getFloat(key);
            if (biome != null && amount > 0f) sand.put(biome, Math.min(1f, amount));
        }
    }
}
