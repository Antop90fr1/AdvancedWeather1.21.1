package net.antopfr.advancedweather.client.foliage;

import net.antopfr.advancedweather.AdvancedWeather;
import net.antopfr.advancedweather.client.render.TerrainUniforms;
import net.antopfr.advancedweather.client.wind.ClientWind;
import net.antopfr.advancedweather.config.AWClientConfig;
import net.antopfr.advancedweather.util.AWLogger;
import net.antopfr.advancedweather.weather.wind.WindField;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

@EventBusSubscriber(modid = AdvancedWeather.MOD_ID, value = Dist.CLIENT)
public final class FoliageSway {

    private FoliageSway() {}

    private static final double WAVE_LENGTH = 32.0;
    private static final double RIPPLE_LENGTH = 18.0;

    private static final double RIPPLE_ANGLE = Math.toRadians(30.0);
    private static final double RIPPLE_PACE = 1.9;
    private static final float RIPPLE_SHARE = 0.3f;
    private static final double FLUTTER_PACE = 2.6;

    private static final double MAX_CAMERA_STEP = 32.0;

    private static double wavePhase;
    private static double ripplePhase;
    private static double flutterPhase;
    private static double lastTime = Double.NaN;
    private static double lastCameraX;
    private static double lastCameraZ;
    private static boolean failed;

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY || failed) return;

        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;

        try {
            upload(level, event.getCamera().getPosition(), event.getPartialTick().getGameTimeDeltaPartialTick(true));
        } catch (Throwable t) {
            failed = true;
            AWLogger.AW.error("[AdvancedWeather] Foliage sway disabled", t);
        }
    }

    private static void upload(ClientLevel level, Vec3 camera, float partialTick) {
        AWClientConfig config = AWClientConfig.get();
        float strength = config.foliageSway ? (float) config.foliageSwayStrength : 0f;
        float intensity = WindField.clientState().intensity();

        Vec3 direction = ClientWind.direction(partialTick);
        double length = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
        double dirX = length > 1.0e-4 ? direction.x / length : 1.0;
        double dirZ = length > 1.0e-4 ? direction.z / length : 0.0;
        double rippleX = dirX * Math.cos(RIPPLE_ANGLE) - dirZ * Math.sin(RIPPLE_ANGLE);
        double rippleZ = dirX * Math.sin(RIPPLE_ANGLE) + dirZ * Math.cos(RIPPLE_ANGLE);
        double k = Mth.TWO_PI / WAVE_LENGTH;
        double rippleK = Mth.TWO_PI / RIPPLE_LENGTH;

        double pace = 0.035 + 0.11 * intensity;
        double now = level.getGameTime() + (double) partialTick;
        if (!Double.isNaN(lastTime)) {
            double elapsed = Mth.clamp(now - lastTime, 0.0, 5.0);
            double movedX = camera.x - lastCameraX;
            double movedZ = camera.z - lastCameraZ;
            if (movedX * movedX + movedZ * movedZ > MAX_CAMERA_STEP * MAX_CAMERA_STEP) {
                movedX = 0.0;
                movedZ = 0.0;
            }
            wavePhase = wrap(wavePhase + k * (movedX * dirX + movedZ * dirZ) - pace * elapsed);
            ripplePhase = wrap(ripplePhase + rippleK * (movedX * rippleX + movedZ * rippleZ) - pace * RIPPLE_PACE * elapsed);
            flutterPhase = wrap(flutterPhase + pace * FLUTTER_PACE * elapsed);
        }
        lastTime = now;
        lastCameraX = camera.x;
        lastCameraZ = camera.z;

        float gust = strength * (0.02f + 0.16f * intensity);
        float lean = strength * 0.12f * intensity * intensity;

        float[] waveA = {(float) (k * dirX), (float) (k * dirZ), (float) wavePhase, gust};
        float[] waveB = {(float) (rippleK * rippleX), (float) (rippleK * rippleZ), (float) ripplePhase, gust * RIPPLE_SHARE};
        float[] windData = {(float) dirX, (float) dirZ, lean, (float) flutterPhase};

        TerrainUniforms.set("AwSwayA", waveA);
        TerrainUniforms.set("AwSwayB", waveB);
        TerrainUniforms.set("AwSwayWind", windData);
    }

    private static double wrap(double radians) {
        return Math.IEEEremainder(radians, Mth.TWO_PI);
    }
}
