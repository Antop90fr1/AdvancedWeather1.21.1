package net.antopfr.advancedweather.weather.effect.types.brimstone_particles;

import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.post.PostPipeline;
import net.antopfr.advancedweather.client.state.ClientWeatherState;
import net.antopfr.advancedweather.util.PlayerChecks;
import net.antopfr.advancedweather.weather.WeatherEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = "advancedweather", value = Dist.CLIENT)
public class BrimstoneOverlay {

    private static final ResourceLocation PIPELINE_ID =
            ResourceLocation.fromNamespaceAndPath("advancedweather", "brimstone_overlay");

    private static final float TWO_PI_100 = (float)(Math.PI * 2.0 * 100.0);

    private static float intensity = 0f;
    private static float time      = 0f;
    private static boolean active  = false;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            disablePipeline();
            return;
        }

        boolean shouldRender = ClientWeatherState.hasEffect(WeatherEffects.BRIMSTONE_PARTICLES)
                && !PlayerChecks.isShielded(mc.level, mc.player.blockPosition());

        float target = (shouldRender ? 1f : 0f);

        intensity = Mth.lerp(0.02f, intensity, target);

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
                .getPipeline(PIPELINE_ID);

        if (pipeline != null) {
            pipeline.getUniformSafe("AWTime").setFloat(time);
            pipeline.getUniformSafe("Intensity").setFloat(intensity);
        }
    }

    private static void enablePipeline() {
        if (active) return;
        active = true;
        VeilRenderSystem.renderer().getPostProcessingManager().add(PIPELINE_ID);
    }

    private static void disablePipeline() {
        if (!active) return;
        active = false;
        VeilRenderSystem.renderer().getPostProcessingManager().remove(PIPELINE_ID);
    }
}
