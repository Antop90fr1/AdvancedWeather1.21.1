package net.antopfr.advancedweather.client.frost;

import net.antopfr.advancedweather.AdvancedWeather;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = AdvancedWeather.MOD_ID, value = Dist.CLIENT)
public final class GroundCover {

    private GroundCover() {}

    private static final float EASE = 1f / (20 * 5);

    private static ClientLevel trackedLevel;
    private static boolean settled;

    private static float snowTold;
    private static float snow;
    private static float lastSnow;

    private static float sandTold;
    private static float sand;
    private static float lastSand;
    private static boolean redSand;

    public static float snow(float partialTick) {
        return Mth.lerp(partialTick, lastSnow, snow);
    }

    public static float sand(float partialTick) {
        return Mth.lerp(partialTick, lastSand, sand);
    }

    public static boolean redSand() {
        return redSand;
    }

    public static void sync(float snowLying, float sandLying, boolean sandIsRed) {
        snowTold = Mth.clamp(snowLying, 0f, 1f);
        sandTold = Mth.clamp(sandLying, 0f, 1f);
        if (sandTold > 0f) redSand = sandIsRed;
        if (!settled) {
            settled = true;
            snow = lastSnow = snowTold;
            sand = lastSand = sandTold;
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            trackedLevel = null;
            snowTold = snow = lastSnow = 0f;
            sandTold = sand = lastSand = 0f;
            settled = false;
            return;
        }
        if (mc.isPaused()) return;

        if (level != trackedLevel) {
            trackedLevel = level;
            snow = lastSnow = snowTold;
            sand = lastSand = sandTold;
            settled = false;
            return;
        }

        lastSnow = snow;
        snow += Mth.clamp(snowTold - snow, -EASE, EASE);
        lastSand = sand;
        sand += Mth.clamp(sandTold - sand, -EASE, EASE);
    }
}
