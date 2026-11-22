package net.antopfr.advancedweather.weather;

import net.antopfr.advancedweather.config.AWCommonConfig;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class Weatherless {

    private Weatherless() {}

    private static List<String> source = List.of();
    private static Set<ResourceLocation> dimensions = Set.of();
    private static Set<String> namespaces = Set.of();

    public static boolean in(Level level) {
        ResourceKey<Level> key = level.dimension();
        if (listed(key.location())) return true;
        if (key.equals(Level.OVERWORLD) || key.equals(Level.NETHER) || key.equals(Level.END)) return false;
        DimensionType type = level.dimensionType();
        return !type.hasSkyLight() || type.hasCeiling();
    }

    private static synchronized boolean listed(ResourceLocation id) {
        List<String> told = AWCommonConfig.get().disabledDimensions;
        if (told == null || told.isEmpty()) return false;
        if (!told.equals(source)) read(told);
        return dimensions.contains(id) || namespaces.contains(id.getNamespace());
    }

    private static void read(List<String> told) {
        Set<ResourceLocation> ids = new HashSet<>();
        Set<String> mods = new HashSet<>();
        for (String entry : told) {
            if (entry == null) continue;
            String clean = entry.trim().toLowerCase(Locale.ROOT);
            if (clean.isEmpty()) continue;
            if (clean.endsWith(":*")) {
                mods.add(clean.substring(0, clean.length() - 2));
                continue;
            }
            ResourceLocation id = ResourceLocation.tryParse(clean);
            if (id != null) ids.add(id);
        }
        dimensions = Set.copyOf(ids);
        namespaces = Set.copyOf(mods);
        source = new ArrayList<>(told);
    }
}
