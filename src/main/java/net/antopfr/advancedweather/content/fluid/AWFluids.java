package net.antopfr.advancedweather.content.fluid;

import com.tterrag.registrate.util.entry.FluidEntry;
import net.antopfr.advancedweather.AdvancedWeather;
import net.antopfr.advancedweather.util.AWRegistrate;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;

public class AWFluids {
    public static final FluidEntry<BaseFlowingFluid.Flowing> MERCURY =
            AWRegistrate.get()
                    .fluid("mercury",
                            ResourceLocation.fromNamespaceAndPath(AdvancedWeather.MOD_ID, "block/mercury_still"),
                            ResourceLocation.fromNamespaceAndPath(AdvancedWeather.MOD_ID, "block/mercury_flow"))
                    .properties(p -> p
                            .density(13500)
                            .viscosity(1500)
                            .canSwim(false)
                            .canDrown(false)
                            .canConvertToSource(false))
                    .noBlock()
                    .noBucket()
                    .fluidProperties(p -> p.bucket(null).block(null))
                    .register();

    public static void register() {}
}
