package net.antopfr.advancedweather.weather.effect.types.chromatic_effect;

import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.post.PostPipeline;
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
public class ChromaticEffect {
    private static final ResourceLocation CHROMATIC_EFFECT_SHADER =
            ResourceLocation.fromNamespaceAndPath("advancedweather", "chromatic_distortion");

    private static final float TWO_PI_100 = (float)(Math.PI * 2.0 * 100.0);

    private static float intensity = 0f;
    private static float time      = 0f;
    private static boolean active  = false;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            disablePipeline();
            return;
        }

        boolean shouldRender = ClientWeatherState.hasEffect(WeatherEffects.CHROMATIC_EFFECT);

        float target = (shouldRender ? 1f : 0f);
        intensity = Mth.lerp(0.015f, intensity, target);

        if (intensity < 0.01f) {
            intensity = 0f;
            disablePipeline();
            return;
        }

        time += 0.016f;
        if (time > TWO_PI_100) time -= TWO_PI_100;

        enablePipeline();

        PostPipeline pipeline = VeilRenderSystem.renderer()
                .getPostProcessingManager()
                .getPipeline(CHROMATIC_EFFECT_SHADER);

        if (pipeline != null) {
            pipeline.getUniformSafe("AWTime").setFloat(time);
            pipeline.getUniformSafe("Intensity").setFloat(intensity);
        }
    }

    private static void enablePipeline() {
        if (FlashbackCompat.editorPreview()) {
            disablePipeline();
            return;
        }
        if (active) return;
        active = true;
        VeilRenderSystem.renderer().getPostProcessingManager().add(CHROMATIC_EFFECT_SHADER);
    }

    private static void disablePipeline() {
        if (!active) return;
        active = false;
        VeilRenderSystem.renderer().getPostProcessingManager().remove(CHROMATIC_EFFECT_SHADER);
    }
}
