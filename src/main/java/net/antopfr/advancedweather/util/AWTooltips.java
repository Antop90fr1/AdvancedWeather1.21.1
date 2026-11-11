package net.antopfr.advancedweather.util;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.List;

public final class AWTooltips {

    private AWTooltips() {}

    public static void append(List<Component> tooltip, String key, int detailLines) {
        append(tooltip, key, detailLines, null);
    }

    public static void append(List<Component> tooltip, String key, int detailLines, String hintKey) {
        tooltip.add(Component.translatable(key + ".summary").withStyle(ChatFormatting.GRAY));

        if (Screen.hasShiftDown()) {
            for (int i = 1; i <= detailLines; i++) {
                String raw = Component.translatable(key + ".detail." + i).getString();
                MutableComponent line;
                if (raw.startsWith("✔")) {
                    line = Component.literal("✔ ").withStyle(ChatFormatting.GREEN)
                            .append(Component.literal(raw.substring(1).trim()).withStyle(ChatFormatting.GRAY));
                } else if (raw.startsWith("✘")) {
                    line = Component.literal("✘ ").withStyle(ChatFormatting.RED)
                            .append(Component.literal(raw.substring(1).trim()).withStyle(ChatFormatting.GRAY));
                } else {
                    line = Component.literal(raw).withStyle(ChatFormatting.DARK_GRAY);
                }
                tooltip.add(line);
            }
            if (hintKey != null) {
                tooltip.add(Component.translatable(hintKey).withStyle(ChatFormatting.BLUE));
            }
        } else {
            tooltip.add(Component.translatable("advancedweather.tooltip.more_info",
                    Component.literal("[Shift]").withStyle(ChatFormatting.GRAY)).withStyle(ChatFormatting.WHITE));
        }
    }

    public static void summary(List<Component> tooltip, String key) {
        tooltip.add(Component.translatable(key).withStyle(ChatFormatting.GRAY));
    }
}
