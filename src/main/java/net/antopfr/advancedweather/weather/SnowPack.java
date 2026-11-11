package net.antopfr.advancedweather.weather;

import net.antopfr.advancedweather.config.AWCommonConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;

import java.util.HashMap;
import java.util.Map;

public final class SnowPack {

    public static final int STEP = 20;

    private final Map<ResourceLocation, Float> snow = new HashMap<>();
    private float crust;

    public float crust() {
        return crust;
    }

    public float snow(ResourceLocation biome) {
        if (biome == null) return 0f;
        Float lying = snow.get(biome);
        return lying == null ? 0f : lying;
    }

    public float snowAt(ServerLevel level, BlockPos pos) {
        if (snow.isEmpty()) return 0f;
        return snow(level.getBiome(pos).unwrapKey().map(ResourceKey::location).orElse(null));
    }

    public void tick(ServerLevel level, WeatherTypes weather, float temperatureC) {
        float minutes = (float) Math.max(0.1, AWCommonConfig.get().snowCoverMinutes);
        float falling = SnowMelt.falling(weather) * STEP / (minutes * 1200f);

        if (snow.isEmpty()) {
            crust = 0f;
            if (falling <= 0f) return;
        } else if (weather == WeatherTypes.FREEZING_RAIN) {
            crust = Math.min(1f, crust + STEP / SnowMelt.CRUST_TICKS);
        } else if (crust > 0f) {
            crust = Math.max(0f, crust - STEP * SnowMelt.crustWear(temperatureC));
        }

        for (ResourceLocation biome : level.registryAccess().registryOrThrow(Registries.BIOME).keySet()) {
            float lying = snow(biome);
            if (lying <= 0f && falling <= 0f) continue;

            float air = temperatureC + BiomeAtmosphereData.getBiomeOffset(level, biome);
            float now = Mth.clamp(lying + falling - STEP * SnowMelt.perTick(air, crust), 0f, 1f);
            if (now > 0f) {
                snow.put(biome, now);
            } else {
                snow.remove(biome);
            }
        }
    }

    public void save(CompoundTag tag) {
        CompoundTag lying = new CompoundTag();
        snow.forEach((biome, amount) -> lying.putFloat(biome.toString(), amount));
        tag.put("snowPack", lying);
        tag.putFloat("snowCrust", crust);
    }

    public void load(CompoundTag tag) {
        snow.clear();
        CompoundTag lying = tag.getCompound("snowPack");
        for (String key : lying.getAllKeys()) {
            ResourceLocation biome = ResourceLocation.tryParse(key);
            float amount = lying.getFloat(key);
            if (biome != null && amount > 0f) snow.put(biome, Math.min(1f, amount));
        }
        crust = Mth.clamp(tag.getFloat("snowCrust"), 0f, 1f);
    }
}
