package net.antopfr.advancedweather.client.wind;

import net.antopfr.advancedweather.AdvancedWeather;
import net.antopfr.advancedweather.weather.wind.WindField;
import net.antopfr.advancedweather.weather.wind.WindSample;
import net.antopfr.advancedweather.weather.wind.WindScale;
import net.antopfr.advancedweather.weather.wind.WindShelter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

@EventBusSubscriber(modid = AdvancedWeather.MOD_ID, value = Dist.CLIENT)
public final class ClientWind {

    private ClientWind() {}

    private static final int SHELTER_INTERVAL = 10;

    private static Vec3 direction = new Vec3(1, 0, 0);
    private static long tick;

    private static float gust;
    private static float localKmh;
    private static float exposure = 1f;
    private static float targetExposure = 1f;
    private static int shelterCountdown;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.isPaused()) return;

        tick++;
        direction = WindField.prevailingDirection(level, 0f);
        WindField.clientState().tickClient(direction);

        Player player = mc.player;
        if (player == null) return;

        if (--shelterCountdown <= 0) {
            shelterCountdown = SHELTER_INTERVAL;
            targetExposure = WindShelter.exposure(level, BlockPos.containing(player.getEyePosition()));
        }
        exposure = Mth.lerp(0.1f, exposure, targetExposure);

        WindSample here = WindField.sample(level, player.getX(), player.getEyeY(), player.getZ());
        gust = Mth.lerp(0.2f, gust, here.gust());
        localKmh = Mth.lerp(0.2f, localKmh, here.speedKmh() * exposure);
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (!(event.getLevel() instanceof ClientLevel)) return;
        WindField.clientState().reset();
        gust = 0f;
        localKmh = 0f;
        exposure = targetExposure = 1f;
    }

    public static long tick() { return tick; }

    public static Vec3 direction() { return direction; }

    public static Vec3 direction(float partialTick) {
        ClientLevel level = Minecraft.getInstance().level;
        return level != null ? WindField.prevailingDirection(level, partialTick) : direction;
    }

    public static float intensity() { return WindField.clientState().intensity(); }

    public static float regionalKmh() { return WindScale.intensityToKmh(intensity()); }

    public static float localKmh() { return localKmh; }

    public static float gust() { return gust; }

    public static float exposure() { return exposure; }

    public static float surge() {
        float amplitude = WindField.gustAmplitude(intensity());
        return (1f - 0.5f * amplitude) + 1.5f * amplitude * gust;
    }

    public static float visibility() {
        float t = Mth.clamp((intensity() - 0.25f) / 0.45f, 0f, 1f);
        float base = t * t * (3f - 2f * t);
        return base * (0.35f + 0.65f * gust) * exposure;
    }
}
