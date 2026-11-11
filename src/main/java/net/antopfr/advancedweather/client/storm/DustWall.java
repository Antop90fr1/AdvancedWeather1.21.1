package net.antopfr.advancedweather.client.storm;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.antopfr.advancedweather.AdvancedWeather;
import net.antopfr.advancedweather.client.wind.ClientWind;
import net.antopfr.advancedweather.config.AWClientConfig;
import net.antopfr.advancedweather.util.PlayerChecks;
import net.antopfr.advancedweather.weather.BiomeFogData;
import net.antopfr.advancedweather.weather.SandDrift;
import net.antopfr.advancedweather.weather.WeatherTypes;
import net.antopfr.advancedweather.weather.effect.types.sand_particles.SandParticleHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

@EventBusSubscriber(modid = AdvancedWeather.MOD_ID, value = Dist.CLIENT)
public final class DustWall {

    private DustWall() {}

    private static final float DISTANCE = 100f;
    private static final float HALF_ARC = 1.15f;
    private static final int SEGMENTS = 28;

    private static final float FOOT = -0.25f;
    private static final float TOP_FAR = 0.05f;
    private static final float TOP_NEAR = 0.55f;
    private static final float SOLID = 0.6f;
    private static final float HAZE = 0.5f;

    private static final float FADE_TICKS = 20 * 6;

    private static final float GRAINS_FROM = 0.7f;
    private static final float GRAINS_SHARE = 0.25f;

    private static boolean sandy;
    private static int colour = 0xD2B280;
    private static int tick;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null) {
            sandy = false;
            return;
        }
        if (mc.isPaused()) return;

        float near = ComingWeather.nearness(WeatherTypes.SANDSTORM, 0f);
        long since = ComingWeather.sinceArrival(WeatherTypes.SANDSTORM);
        if (near <= 0f && (since < 0 || since >= FADE_TICKS)) {
            sandy = false;
            return;
        }
        Holder<Biome> biome = level.getBiome(mc.player.blockPosition());
        sandy = SandDrift.isSandy(biome);
        colour = BiomeFogData.getColor(level, WeatherTypes.SANDSTORM,
                biome.unwrapKey().map(ResourceKey::location).orElse(null));

        if (!sandy || near < GRAINS_FROM) return;
        if (++tick % Math.max(1, AWClientConfig.get().sandTickInterval) != 0) return;
        if (PlayerChecks.isShielded(level, mc.player.blockPosition())) return;
        SandParticleHandler.blow(mc, GRAINS_SHARE * (near - GRAINS_FROM) / (1f - GRAINS_FROM));
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || !sandy) return;
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;

        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(true);
        float near = ComingWeather.nearness(WeatherTypes.SANDSTORM, partialTick);
        float left = 1f;
        if (near <= 0f) {
            long since = ComingWeather.sinceArrival(WeatherTypes.SANDSTORM);
            if (since < 0 || since >= FADE_TICKS) return;
            float gone = Mth.clamp((since + partialTick) / FADE_TICKS, 0f, 1f);
            left = 1f - gone * gone * (3f - 2f * gone);
            near = 1f;
        }

        Vec3 wind = ClientWind.direction(partialTick);
        double length = Math.sqrt(wind.x * wind.x + wind.z * wind.z);
        float x = length < 1.0e-4 ? 1f : (float) (-wind.x / length);
        float z = length < 1.0e-4 ? 0f : (float) (-wind.z / length);

        float top = Mth.lerp(near, TOP_FAR, TOP_NEAR);
        float solid = Mth.lerp(near, 0.35f, 0.9f) * left;
        float haze = Mth.lerp(near, 0.06f, 0.3f) * left;

        float light = level.getSkyDarken(partialTick);
        int red = (int) (FastColor.ARGB32.red(colour) * light);
        int green = (int) (FastColor.ARGB32.green(colour) * light);
        int blue = (int) (FastColor.ARGB32.blue(colour) * light);

        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (int i = 0; i < SEGMENTS; i++) {
            float from = -HALF_ARC + 2f * HALF_ARC * i / SEGMENTS;
            float to = -HALF_ARC + 2f * HALF_ARC * (i + 1) / SEGMENTS;
            float fromSide = side(from);
            float toSide = side(to);

            band(buffer, x, z, from, to, FOOT, top * SOLID, red, green, blue,
                    solid * fromSide, solid * toSide, solid * fromSide, solid * toSide);
            band(buffer, x, z, from, to, top * SOLID, top, red, green, blue,
                    solid * fromSide, solid * toSide, haze * fromSide, haze * toSide);
            band(buffer, x, z, from, to, top, top + HAZE, red, green, blue,
                    haze * fromSide, haze * toSide, 0f, 0f);
        }
        MeshData mesh = buffer.build();
        if (mesh != null) BufferUploader.drawWithShader(mesh);

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private static float side(float angle) {
        float out = Mth.clamp((Math.abs(angle) / HALF_ARC - 0.55f) / 0.45f, 0f, 1f);
        return 1f - out * out * (3f - 2f * out);
    }

    private static void band(BufferBuilder buffer, float x, float z, float from, float to, float low, float high,
                             int red, int green, int blue,
                             float lowFrom, float lowTo, float highFrom, float highTo) {
        vertex(buffer, x, z, from, low, red, green, blue, lowFrom);
        vertex(buffer, x, z, to, low, red, green, blue, lowTo);
        vertex(buffer, x, z, to, high, red, green, blue, highTo);
        vertex(buffer, x, z, from, high, red, green, blue, highFrom);
    }

    private static void vertex(BufferBuilder buffer, float x, float z, float bearing, float elevation,
                               int red, int green, int blue, float alpha) {
        float flat = Mth.cos(elevation) * DISTANCE;
        float dx = x * Mth.cos(bearing) - z * Mth.sin(bearing);
        float dz = x * Mth.sin(bearing) + z * Mth.cos(bearing);
        buffer.addVertex(dx * flat, Mth.sin(elevation) * DISTANCE, dz * flat)
                .setColor(red, green, blue, Mth.clamp((int) (alpha * 255f), 0, 255));
    }
}
