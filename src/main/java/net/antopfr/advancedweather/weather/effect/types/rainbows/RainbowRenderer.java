package net.antopfr.advancedweather.weather.effect.types.rainbows;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.antopfr.advancedweather.AdvancedWeather;
import net.antopfr.advancedweather.client.cloud.CloudState;
import net.antopfr.advancedweather.client.render.SkyVeil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Vector3f;

@EventBusSubscriber(modid = AdvancedWeather.MOD_ID, value = Dist.CLIENT)
public final class RainbowRenderer {

    private RainbowRenderer() {}

    private static final float PRIMARY_MIN = 40.0f;
    private static final float PRIMARY_MAX = 42.4f;
    private static final float SECONDARY_MIN = 50.6f;
    private static final float SECONDARY_MAX = 53.4f;
    private static final float GLOW_WIDTH = 7f;

    private static final float PRIMARY_ALPHA = 0.8f;
    private static final float SECONDARY_ALPHA = 0.28f;
    private static final float GLOW_ALPHA = 0.1f;

    private static final float UNDER_CLOUDS = 0.9f;
    private static final float NEAREST = 8f;
    private static final float FARTHEST = 0.9f;
    private static final int SEGMENTS = 72;
    private static final int RINGS = 14;

    private static final int[] SPECTRUM = {
            0x6B29CE, 0x1F1FE0, 0x1F71E2, 0x78E520, 0xFFCD05, 0xFF661C, 0xE22E2B
    };

    private static float sheet;
    private static float far;

    private static long start;
    private static long end;

    public static void setEvent(long start, long end) {
        RainbowRenderer.start = start;
        RainbowRenderer.end = end;
    }

    public static float intensity(ClientLevel level, float partialTick) {
        if (start >= end || !level.dimension().equals(Level.OVERWORLD)) return 0f;

        float envelope = Rainbow.envelope((double) level.getGameTime() + partialTick, start, end);
        if (envelope <= 0f) return 0f;
        float sun = Rainbow.sunFactor(Rainbow.sunElevationDeg(level.getTimeOfDay(partialTick)));
        return envelope * sun * SkyVeil.visibility(partialTick);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;

        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(true);
        float intensity = intensity(level, partialTick);
        if (intensity < 0.01f) return;
        intensity *= 0.94f + 0.06f * Mth.sin((level.getGameTime() % 15708 + partialTick) * 0.04f);

        float sunAngle = level.getSunAngle(partialTick);
        Vector3f axis = new Vector3f(Mth.sin(sunAngle), -Mth.cos(sunAngle), 0f);
        Vector3f right = new Vector3f(0f, 0f, 1f);
        Vector3f up = new Vector3f(axis).cross(right);
        if (up.y < 0f) {
            up.negate();
            right.negate();
        }

        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Minecraft mc = Minecraft.getInstance();
        sheet = Math.max(0f, (float) (CloudState.altitude() - event.getCamera().getPosition().y) * UNDER_CLOUDS);
        far = mc.gameRenderer.getDepthFar() * FARTHEST;

        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        glow(buffer, axis, right, up, intensity * GLOW_ALPHA);
        bow(buffer, axis, right, up, PRIMARY_MIN, PRIMARY_MAX, false, intensity * PRIMARY_ALPHA);
        bow(buffer, axis, right, up, SECONDARY_MIN, SECONDARY_MAX, true, intensity * SECONDARY_ALPHA);
        MeshData mesh = buffer.build();
        if (mesh != null) BufferUploader.drawWithShader(mesh);

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private static void bow(BufferBuilder buffer, Vector3f axis, Vector3f right, Vector3f up,
                            float innerDeg, float outerDeg, boolean reversed, float alpha) {
        for (int ring = 0; ring < RINGS; ring++) {
            float t0 = ring / (float) RINGS;
            float t1 = (ring + 1) / (float) RINGS;
            band(buffer, axis, right, up,
                    Mth.lerp(t0, innerDeg, outerDeg), Mth.lerp(t1, innerDeg, outerDeg),
                    spectrum(reversed ? 1f - t0 : t0), spectrum(reversed ? 1f - t1 : t1),
                    alpha * softEdge(t0), alpha * softEdge(t1));
        }
    }

    private static void glow(BufferBuilder buffer, Vector3f axis, Vector3f right, Vector3f up, float alpha) {
        band(buffer, axis, right, up, PRIMARY_MIN - GLOW_WIDTH, PRIMARY_MIN + 0.4f, 0xFFFFFF, 0xFFFFFF, 0f, alpha);
    }

    private static void band(BufferBuilder buffer, Vector3f axis, Vector3f right, Vector3f up,
                             float innerDeg, float outerDeg, int innerRgb, int outerRgb,
                             float innerAlpha, float outerAlpha) {
        float inner = innerDeg * Mth.DEG_TO_RAD;
        float outer = outerDeg * Mth.DEG_TO_RAD;
        Vector3f a = new Vector3f(), b = new Vector3f(), c = new Vector3f(), d = new Vector3f();

        for (int segment = 0; segment < SEGMENTS; segment++) {
            float theta0 = Mth.PI * segment / SEGMENTS;
            float theta1 = Mth.PI * (segment + 1) / SEGMENTS;
            direction(axis, right, up, inner, theta0, a);
            direction(axis, right, up, outer, theta0, b);
            direction(axis, right, up, outer, theta1, c);
            direction(axis, right, up, inner, theta1, d);
            if (a.y <= 0f && b.y <= 0f && c.y <= 0f && d.y <= 0f) continue;

            vertex(buffer, a, innerRgb, innerAlpha);
            vertex(buffer, b, outerRgb, outerAlpha);
            vertex(buffer, c, outerRgb, outerAlpha);
            vertex(buffer, d, innerRgb, innerAlpha);
        }
    }

    private static void direction(Vector3f axis, Vector3f right, Vector3f up, float angle, float theta, Vector3f out) {
        float spread = Mth.sin(angle);
        out.set(axis).mul(Mth.cos(angle))
                .add(right.x * Mth.cos(theta) * spread, right.y * Mth.cos(theta) * spread, right.z * Mth.cos(theta) * spread)
                .add(up.x * Mth.sin(theta) * spread, up.y * Mth.sin(theta) * spread, up.z * Mth.sin(theta) * spread);
    }

    private static void vertex(BufferBuilder buffer, Vector3f direction, int rgb, float alpha) {
        float horizon = Mth.clamp(direction.y * 9f, 0f, 1f);
        int a = Mth.clamp((int) (alpha * horizon * 255f), 0, 255);
        float distance = sheet > 0f && direction.y > 1.0e-3f
                ? Mth.clamp(sheet / direction.y, NEAREST, far)
                : far;
        buffer.addVertex(direction.x * distance, direction.y * distance, direction.z * distance)
                .setColor(FastColor.ARGB32.red(rgb), FastColor.ARGB32.green(rgb), FastColor.ARGB32.blue(rgb), a);
    }

    private static float softEdge(float t) {
        return Mth.clamp(Math.min(t, 1f - t) / 0.18f, 0f, 1f);
    }

    private static int spectrum(float t) {
        float scaled = Mth.clamp(t, 0f, 1f) * (SPECTRUM.length - 1);
        int index = Math.min((int) scaled, SPECTRUM.length - 2);
        return FastColor.ARGB32.lerp(scaled - index, SPECTRUM[index], SPECTRUM[index + 1]);
    }
}
