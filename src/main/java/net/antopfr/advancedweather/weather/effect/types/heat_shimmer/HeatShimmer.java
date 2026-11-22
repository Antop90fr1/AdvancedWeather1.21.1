package net.antopfr.advancedweather.weather.effect.types.heat_shimmer;

import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.post.PostPipeline;
import net.antopfr.advancedweather.client.state.ClientAtmosphereState;
import net.antopfr.advancedweather.client.state.ClientWeatherState;
import net.antopfr.advancedweather.compat.flashback.FlashbackCompat;
import net.antopfr.advancedweather.weather.WeatherEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = "advancedweather", value = Dist.CLIENT)
public class HeatShimmer {

    private static final ResourceLocation HEAT_SHIMMER_SHADER =
            ResourceLocation.fromNamespaceAndPath("advancedweather", "heat_shimmer");

    private static final float TEMP_MAX   = 110f;
    private static final float TEMP_MIN   = 40f;
    private static final float TWO_PI_100 = (float)(Math.PI * 2.0 * 100.0);

    private static float intensity      = 0f;
    private static float smoothedTemp   = TEMP_MIN;
    private static float time           = 0f;
    private static boolean active       = false;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            disablePipeline();
            return;
        }

        boolean shouldRender = ClientWeatherState.hasEffect(WeatherEffects.HEAT_SHIMMER);

        float localTemp = ClientAtmosphereState.getLocalTemperature();

        smoothedTemp = Mth.lerp(0.05f, smoothedTemp, localTemp);

        boolean tempInRange = smoothedTemp >= TEMP_MIN;

        float target = (shouldRender && tempInRange)
                ? Mth.clamp((smoothedTemp - TEMP_MIN) / (TEMP_MAX - TEMP_MIN), 0f, 1f)
                : 0f;

        intensity = Mth.lerp(0.03f, intensity, target);

        if (intensity < 0.005f) {
            intensity = 0f;
            disablePipeline();
            return;
        }

        time += 0.016f;
        if (time > TWO_PI_100) time -= TWO_PI_100;

        enablePipeline();

        PostPipeline pipeline = VeilRenderSystem.renderer()
                .getPostProcessingManager()
                .getPipeline(HEAT_SHIMMER_SHADER);

        if (pipeline != null) {
            pipeline.getUniformSafe("AWTime").setFloat(time);
            pipeline.getUniformSafe("Intensity").setFloat(intensity);
            pipeline.getUniformSafe("Temperature").setFloat(smoothedTemp);
        }
    }

    private static void enablePipeline() {
        if (FlashbackCompat.editorPreview()) {
            disablePipeline();
            return;
        }
        if (active) return;
        active = true;
        VeilRenderSystem.renderer().getPostProcessingManager().add(HEAT_SHIMMER_SHADER);
    }

    private static void disablePipeline() {
        if (!active) return;
        active = false;
        VeilRenderSystem.renderer().getPostProcessingManager().remove(HEAT_SHIMMER_SHADER);
    }
}