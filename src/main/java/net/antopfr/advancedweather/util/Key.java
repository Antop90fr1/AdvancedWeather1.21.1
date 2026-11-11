package net.antopfr.advancedweather.util;

import net.minecraft.network.chat.Component;

public class Key {
    public static String t(String key) {
        return Component.translatable(key).getString();
    }

    public static String label(String key) {
        return Component.translatable("advancedweather.label", Component.translatable(key)).getString();
    }

    public static String c(String color, String key) {
        return color + t(key) + "§r";
    }
}
