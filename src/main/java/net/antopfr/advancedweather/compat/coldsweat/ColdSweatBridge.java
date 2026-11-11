package net.antopfr.advancedweather.compat.coldsweat;

import com.momosoftworks.coldsweat.api.event.core.init.GatherDefaultTempModifiersEvent;
import com.momosoftworks.coldsweat.api.event.core.registry.TempModifierRegisterEvent;
import com.momosoftworks.coldsweat.api.temperature.modifier.compat.StormTempModifier;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.api.util.placement.Matcher;
import net.antopfr.advancedweather.AdvancedWeather;
import net.antopfr.advancedweather.config.AWCommonConfig;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.NeoForge;

final class ColdSweatBridge {

    private ColdSweatBridge() {}

    private static final ResourceLocation MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(AdvancedWeather.MOD_ID, "weather");

    static void register() {
        NeoForge.EVENT_BUS.addListener(ColdSweatBridge::onRegisterModifiers);
        NeoForge.EVENT_BUS.addListener(ColdSweatBridge::onGatherDefaults);
    }

    private static void onRegisterModifiers(TempModifierRegisterEvent event) {
        event.register(MODIFIER_ID, AWWeatherTempModifier::new);
    }

    private static void onGatherDefaults(GatherDefaultTempModifiersEvent event) {
        if (event.getTrait() != Temperature.Trait.WORLD) return;

        event.removeModifiers(new AWWeatherTempModifier(), Matcher.SAME_CLASS);
        if (!AWCommonConfig.get().coldSweatIntegration) return;

        if (AWCommonConfig.get().coldSweatReplaceStormModifier) {
            event.removeModifiers(new StormTempModifier(), Matcher.SAME_CLASS);
        }

        event.addModifier(new AWWeatherTempModifier());
    }
}
