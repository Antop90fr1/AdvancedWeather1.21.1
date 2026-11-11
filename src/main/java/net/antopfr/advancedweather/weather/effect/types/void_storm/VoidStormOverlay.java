package net.antopfr.advancedweather.weather.effect.types.void_storm;

import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.post.PostPipeline;
import net.antopfr.advancedweather.client.state.ClientWeatherState;
import net.antopfr.advancedweather.util.PlayerChecks;
import net.antopfr.advancedweather.weather.WeatherTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = "advancedweather", value = Dist.CLIENT)
public final class VoidStormOverlay {

    private VoidStormOverlay() {}

    private static final ResourceLocation PIPELINE_ID =
            ResourceLocation.fromNamespaceAndPath("advancedweather", "void_overlay");

    private static float intensity = 0f;
    private static boolean active = false;
    private static VoidDroneSound drone;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            disablePipeline();
            return;
        }

        float presence = mc.level.dimension().equals(Level.END)
                ? ClientWeatherState.presence(WeatherTypes.VOID_STORM, 1f)
                : 0f;

        if (presence > 0f && (drone == null || !mc.getSoundManager().isActive(drone))) {
            drone = new VoidDroneSound();
            mc.getSoundManager().play(drone);
        }

        boolean outside = !PlayerChecks.isShielded(mc.level, mc.player.blockPosition());
        intensity = Mth.lerp(0.05f, intensity, outside ? presence : 0f);

        if (intensity < 0.01f) {
            intensity = 0f;
            disablePipeline();
            return;
        }

        enablePipeline();

        PostPipeline pipeline = VeilRenderSystem.renderer()
                .getPostProcessingManager()
                .getPipeline(PIPELINE_ID);

        if (pipeline != null) {
            pipeline.getUniformSafe("Intensity").setFloat(intensity);
            pipeline.getUniformSafe("Surge").setFloat(VoidPulse.surge(mc.level.getGameTime(), 1f));
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
