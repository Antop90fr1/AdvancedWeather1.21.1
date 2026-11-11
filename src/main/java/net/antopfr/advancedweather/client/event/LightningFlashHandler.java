package net.antopfr.advancedweather.client.event;

import net.antopfr.advancedweather.client.state.ClientWeatherState;
import net.antopfr.advancedweather.weather.WeatherTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

import java.util.Random;

@EventBusSubscriber(modid = "advancedweather", value = Dist.CLIENT)
public class LightningFlashHandler {

    private static final Random random = new Random();
    private static int flashTick = 0;
    private static final int FLASH_DURATION = 2;

    private static int shakeTick = 0;
    private static final int SHAKE_DURATION = 8;
    private static float shakeIntensity = 0f;
    private static float dazzle = 1f;

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Pre event) {
        if (flashTick <= 0) return;

        WeatherTypes current = ClientWeatherState.getCurrentWeather();
        if (current != WeatherTypes.THUNDERSTORM && current != WeatherTypes.NETHERSTORM && current != WeatherTypes.ENDERSTORM) {
            flashTick = 0;
            return;
        }

        float alpha = (float) flashTick / FLASH_DURATION * 0.85f * dazzle;

        GuiGraphics graphics = event.getGuiGraphics();
        int w = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int h = Minecraft.getInstance().getWindow().getGuiScaledHeight();

        graphics.fill(0, 0, w, h,
                (int)(alpha * 255) << 24 | 0x00FFFFFF
        );
    }

    public static float flashStrength() {
        return flashTick / (float) FLASH_DURATION * dazzle;
    }

    public static void triggerFlash(float strength) {
        dazzle = Math.max(0f, Math.min(1f, strength));
        flashTick = FLASH_DURATION;
        shakeTick = SHAKE_DURATION;
        shakeIntensity = (0.8f + random.nextFloat() * 0.4f) * dazzle;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (flashTick > 0) flashTick--;
        if (shakeTick > 0) shakeTick--;
    }

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (shakeTick <= 0) return;
        float t = (float) shakeTick / SHAKE_DURATION;
        float intensity = shakeIntensity * t;
        event.setYaw(event.getYaw()     + (random.nextFloat() - 0.5f) * intensity * 2f);
        event.setPitch(event.getPitch() + (random.nextFloat() - 0.5f) * intensity);
    }
}
