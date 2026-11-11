package net.antopfr.advancedweather.content.item;

import net.antopfr.advancedweather.AdvancedWeather;
import net.antopfr.advancedweather.config.AWCommonConfig;
import net.antopfr.advancedweather.content.AWDataComponents;
import net.antopfr.advancedweather.content.item.almanac.WeatherAlmanacItem;
import net.antopfr.advancedweather.content.item.kite.KiteArmPose;
import net.antopfr.advancedweather.content.item.kite.KiteColors;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import org.jetbrains.annotations.NotNull;

@EventBusSubscriber(modid = "advancedweather", value = Dist.CLIENT)
public class AWItemProperties {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemProperties.register(
                AWItems.WEATHER_ALMANAC.get(),
                ResourceLocation.fromNamespaceAndPath("advancedweather", "filled"),
                (stack, level, entity, seed) -> {
                    int count = WeatherAlmanacItem.getRecords(stack).size();
                    int max = AWCommonConfig.get().almanacMaxRecords;
                    return count >= max ? 1.0f : 0.0f;
                }));
        event.enqueueWork(() -> ItemProperties.register(
                AWItems.KITE_ITEM.get(),
                ResourceLocation.fromNamespaceAndPath(AdvancedWeather.MOD_ID, "deployed"),
                (stack, level, entity, seed) ->
                        stack.getOrDefault(AWDataComponents.KITE_DEPLOYED.get(), false) ? 1f : 0f));
    }

    @SubscribeEvent
    public static void registerItemExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            @Override
            public HumanoidModel.ArmPose getArmPose(@NotNull LivingEntity entity,
                                                    @NotNull InteractionHand hand,
                                                    @NotNull ItemStack stack) {
                return stack.getOrDefault(AWDataComponents.KITE_DEPLOYED.get(), false)
                        ? KiteArmPose.get()
                        : HumanoidModel.ArmPose.EMPTY;
            }
        }, AWItems.KITE_ITEM.get());
    }

    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, layer) -> {
            KiteColors c = stack.getOrDefault(
                    AWDataComponents.KITE_COLORS.get(), KiteColors.WHITE);
            return 0xFF000000 | switch (layer) {
                case 1 -> c.topLeft();
                case 2 -> c.topRight();
                case 3 -> c.bottomLeft();
                case 4 -> c.bottomRight();
                default -> 0xFFFFFF;
            };
        }, AWItems.KITE_ITEM.get());
    }
}
