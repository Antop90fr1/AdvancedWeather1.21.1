package net.antopfr.advancedweather.content.item;

import net.antopfr.advancedweather.client.render.AWClientRenderers;
import net.antopfr.advancedweather.client.state.ClientAtmosphereState;
import net.antopfr.advancedweather.util.Key;
import net.antopfr.advancedweather.util.ValueColors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.Mth;
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

public class HygrometerItem extends Item {
    public HygrometerItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        if (level.isClientSide) {
            float hum = ClientAtmosphereState.getLocalHumidity();
            String hLabel = ValueColors.humidityLabel(hum);

            int rgbColor = ValueColors.humidity(hum);

            MutableComponent prefix = Component.literal("§7" + Key.label("advancedweather.air_humidity"));
            MutableComponent humValue = Component.literal(String.format("%.0f%%", hum))
                    .withStyle(style -> style.withColor(TextColor.fromRgb(rgbColor)));
            MutableComponent suffix = Component.literal(String.format(" §7| §f[%s]", hLabel));

            player.displayClientMessage(prefix.append(humValue).append(suffix), true);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context,
                                @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        if (Minecraft.getInstance().level != null) {
            float hum = ClientAtmosphereState.getLocalHumidity();
            String hLabel = ValueColors.humidityLabel(hum);
            int rgbColor = ValueColors.humidity(hum);

            MutableComponent prefix = Component.literal("§7" + Key.label("advancedweather.humidity"));
            MutableComponent humValue = Component.literal(String.format("%.0f%%", hum))
                    .withStyle(style -> style.withColor(TextColor.fromRgb(rgbColor)));
            MutableComponent suffix = Component.literal(String.format(" §7(%s)", hLabel));

            tooltip.add(prefix.append(humValue).append(suffix));
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
