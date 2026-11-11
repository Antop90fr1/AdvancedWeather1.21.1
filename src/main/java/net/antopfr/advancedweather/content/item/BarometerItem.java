package net.antopfr.advancedweather.content.item;

import net.antopfr.advancedweather.client.render.AWClientRenderers;
import net.antopfr.advancedweather.client.state.ClientAtmosphereState;
import net.antopfr.advancedweather.util.Key;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Consumer;

public class BarometerItem extends Item {
    public BarometerItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        if (level.isClientSide) {
            float pressure = ClientAtmosphereState.getLocalPressure();
            float trend = ClientAtmosphereState.getTrend() * 20 * 60;

            String trendSym = trend > 0.002f ? "§a↑" : trend < -0.002f ? "§c↓" : "§7→";

            player.displayClientMessage(
                    Component.literal("§7" + Key.label("advancedweather.pressure")
                            + String.format("§f%.1f hPa §8(%s§8)", pressure, trendSym)),
                    true
            );
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context,
                                @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        if (Minecraft.getInstance().level != null) {
            float pressure = ClientAtmosphereState.getLocalPressure();
            float trend = ClientAtmosphereState.getTrend() * 20 * 60;
            String trendSym = trend > 0.002f ? "§a↑" : trend < -0.002f ? "§c↓" : "§7→";

            tooltip.add(Component.literal("§7" + Key.label("advancedweather.pressure")
                            + String.format("§f%.1f hPa §8(%s§8)", pressure, trendSym)));
        }
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return AWClientRenderers.INSTRUMENT_RENDERER.get();
            }
        });
    }
}
