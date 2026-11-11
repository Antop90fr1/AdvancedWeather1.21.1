package net.antopfr.advancedweather.content.item;

import net.antopfr.advancedweather.content.AWDataComponents;
import net.antopfr.advancedweather.content.WeatherRecord;
import net.antopfr.advancedweather.util.Key;
import net.antopfr.advancedweather.util.ValueColors;
import net.antopfr.advancedweather.weather.wind.WindScale;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class WeatherReportItem extends Item {
    public WeatherReportItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context,
                                @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        WeatherRecord r = stack.get(AWDataComponents.WEATHER_RECORD.get());
        if (r == null) {
            tooltip.add(Component.translatable("advancedweather.report.blank"));
            return;
        }

        long day = r.gameTime() / 24000L;
        int hours = (int) ((r.gameTime() + 6000) / 1000 % 24);
        int minutes = (int) ((r.gameTime() % 1000) * 60 / 1000);
        tooltip.add(Component.translatable("advancedweather.time.day_clock", day,
                String.format("%02d:%02d", hours, minutes)).withStyle(ChatFormatting.GRAY));

        tooltip.add(r.weatherType().displayName().copy().withStyle(ChatFormatting.WHITE));

        if (r.hasTemperature()) {
            float t = r.temperature();
            tooltip.add(Component.literal("§7" + Key.label("advancedweather.temperature"))
                    .append(Component.literal(String.format("%.1f °C", t))
                            .withStyle(s -> s.withColor(TextColor.fromRgb(ValueColors.temperature(t))))));
        } else {
            tooltip.add(Component.literal("§7" + Key.label("advancedweather.temperature") + "§8-"));
        }

        if (r.hasPressure()) {
            tooltip.add(Component.literal("§7" + Key.label("advancedweather.pressure") + String.format("§f%.1f hPa", r.pressure())));
        } else {
            tooltip.add(Component.literal("§7" + Key.label("advancedweather.pressure") + "§8-"));
        }

        if (r.hasHumidity()) {
            float h = r.humidity();
            tooltip.add(Component.literal("§7" + Key.label("advancedweather.humidity"))
                    .append(Component.literal(String.format("%.0f%%", h))
                            .withStyle(s -> s.withColor(TextColor.fromRgb(ValueColors.humidity(h)))))
                    .append(Component.literal(" §8[" + ValueColors.humidityLabel(h) + "]")));
        } else {
            tooltip.add(Component.literal("§7" + Key.label("advancedweather.humidity") + "§8-"));
        }

        if (r.hasWind()) {
            float kmh = WindScale.intensityToKmh(r.windIntensity());
            tooltip.add(Component.literal("§7" + Key.label("advancedweather.wind"))
                    .append(Component.literal(String.format("%.1f km/h", kmh))
                            .withStyle(s -> s.withColor(TextColor.fromRgb(ValueColors.wind(kmh)))))
                    .append(Component.literal(" §8[" + WindScale.beaufort(kmh).getString() + "]")));
        } else {
            tooltip.add(Component.literal("§7" + Key.label("advancedweather.wind") + "§8-"));
        }

        if (r.stationName().isBlank()) {
            tooltip.add(Component.literal(String.format("§8%s @ %d, %d, %d",
                    r.biome().getPath(), r.pos().getX(), r.pos().getY(), r.pos().getZ())));
        } else {
            tooltip.add(Component.literal("§b" + r.stationName()));
        }
    }
}
