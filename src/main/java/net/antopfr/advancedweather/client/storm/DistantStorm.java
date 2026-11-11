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
import net.antopfr.advancedweather.weather.StormApproach;
import net.antopfr.advancedweather.weather.WeatherTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@EventBusSubscriber(modid = AdvancedWeather.MOD_ID, value = Dist.CLIENT)
public final class DistantStorm {

    private DistantStorm() {}

    private static final WeatherTypes[] STORMS = {
            WeatherTypes.THUNDERSTORM, WeatherTypes.NETHERSTORM, WeatherTypes.ENDERSTORM
    };

    private static final float SPEED = 600f / StormApproach.LEAD_TICKS;
    private static final float SOUND_SPEED = 2f;

    private static final int GAP_FAR = 240;
    private static final int GAP_NEAR = 80;
    private static final int FLASH_TICKS = 5;
    private static final float SPREAD = 0.9f;

    private static final float RUMBLE_AT = 5f;

    private static final float DISTANCE = 100f;
    private static final float GLOW_HEIGHT = 0.10f;
    private static final float GLOW_WIDE = 0.55f;
    private static final float GLOW_TALL = 0.20f;
    private static final float GLOW_ALPHA = 0.5f;
    private static final int GLOW_SEGMENTS = 20;

    private static final int SKY_LIGHTNING = 0xC8D4FF;
    private static final int NETHER_LIGHTNING = 0xFF9A5A;
    private static final int END_LIGHTNING = 0xC88CFF;

    private static final class Rumble {
        private int ticksLeft;
        private final float volume;
        private final float pitch;
        private final float x;
        private final float z;

        private Rumble(int ticksLeft, float volume, float pitch, float x, float z) {
            this.ticksLeft = ticksLeft;
            this.volume = volume;
            this.pitch = pitch;
            this.x = x;
            this.z = z;
        }
    }

    private static final RandomSource RANDOM = RandomSource.create();
    private static final List<Rumble> rumbles = new ArrayList<>();

    private static int untilFlash = 40;
    private static int flashLeft;
    private static float flashPeak;
    private static float flashX = 1f;
    private static float flashZ;
    private static int flashColour = SKY_LIGHTNING;

    public static float flash(float partialTick) {
        if (flashLeft <= 0) return 0f;
        float left = Mth.clamp((flashLeft - partialTick) / FLASH_TICKS, 0f, 1f);
        float flicker = 0.65f + 0.35f * Mth.cos(left * 14f);
        return Mth.clamp(flashPeak * left * flicker, 0f, 1f);
    }

    public static float flashX() {
        return flashX;
    }

    public static float flashZ() {
        return flashZ;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            flashLeft = 0;
            rumbles.clear();
            return;
        }
        if (mc.isPaused()) return;

        if (flashLeft > 0) flashLeft--;
        rumble(mc);

        long away = ComingWeather.ticksAway(STORMS);
        if (away < 0 || --untilFlash > 0) return;

        float near = 1f - away / (float) StormApproach.LEAD_TICKS;
        untilFlash = Math.round(Mth.lerp(near, GAP_FAR, GAP_NEAR) * (0.6f + 0.8f * RANDOM.nextFloat()));
        strike(near, away, ComingWeather.weather());
    }

    private static void strike(float near, long away, WeatherTypes storm) {
        Vec3 wind = ClientWind.direction();
        double length = Math.sqrt(wind.x * wind.x + wind.z * wind.z);
        double x = length < 1.0e-4 ? 1.0 : -wind.x / length;
        double z = length < 1.0e-4 ? 0.0 : -wind.z / length;

        float turn = (RANDOM.nextFloat() - 0.5f) * SPREAD;
        flashX = (float) (x * Mth.cos(turn) - z * Mth.sin(turn));
        flashZ = (float) (x * Mth.sin(turn) + z * Mth.cos(turn));
        flashPeak = Mth.lerp(near, 0.4f, 1f) * (0.7f + 0.3f * RANDOM.nextFloat());
        flashLeft = FLASH_TICKS;
        flashColour = storm == WeatherTypes.NETHERSTORM ? NETHER_LIGHTNING
                : storm == WeatherTypes.ENDERSTORM ? END_LIGHTNING
                : SKY_LIGHTNING;

        int delay = Math.round(away * SPEED / SOUND_SPEED);
        float volume = Mth.lerp(near, 0.25f, 1f);
        float depth = storm == WeatherTypes.NETHERSTORM ? 0.8f : 1f;
        float pitch = (Mth.lerp(near, 0.55f, 0.85f) + RANDOM.nextFloat() * 0.1f) * depth;
        rumbles.add(new Rumble(delay, volume, pitch, flashX, flashZ));
    }

    private static void rumble(Minecraft mc) {
        Player player = mc.player;
        for (Iterator<Rumble> pending = rumbles.iterator(); pending.hasNext(); ) {
            Rumble rumble = pending.next();
            if (--rumble.ticksLeft > 0) continue;
            pending.remove();
            if (player == null) continue;

            mc.getSoundManager().play(new SimpleSoundInstance(SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER,
                    rumble.volume, rumble.pitch, RandomSource.create(),
                    player.getX() + rumble.x * RUMBLE_AT, player.getEyeY() + 2.0, player.getZ() + rumble.z * RUMBLE_AT));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        if (Minecraft.getInstance().level == null) return;

        float strength = flash(event.getPartialTick().getGameTimeDeltaPartialTick(true));
        if (strength < 0.01f) return;

        Vector3f centre = new Vector3f(flashX, GLOW_HEIGHT, flashZ).normalize();
        Vector3f right = new Vector3f(-flashZ, 0f, flashX);
        int alpha = Mth.clamp((int) (strength * GLOW_ALPHA * 255f), 0, 255);

        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (int i = 0; i < GLOW_SEGMENTS; i++) {
            Vector3f from = rim(centre, right, i);
            Vector3f to = rim(centre, right, i + 1);
            vertex(buffer, centre, alpha);
            vertex(buffer, centre, alpha);
            vertex(buffer, from, 0);
            vertex(buffer, to, 0);
        }
        MeshData mesh = buffer.build();
        if (mesh != null) BufferUploader.drawWithShader(mesh);

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private static Vector3f rim(Vector3f centre, Vector3f right, int segment) {
        float angle = Mth.TWO_PI * segment / GLOW_SEGMENTS;
        return new Vector3f(centre)
                .add(right.x * Mth.cos(angle) * GLOW_WIDE, Mth.sin(angle) * GLOW_TALL, right.z * Mth.cos(angle) * GLOW_WIDE)
                .normalize();
    }

    private static void vertex(BufferBuilder buffer, Vector3f direction, int alpha) {
        buffer.addVertex(direction.x * DISTANCE, direction.y * DISTANCE, direction.z * DISTANCE)
                .setColor(FastColor.ARGB32.red(flashColour), FastColor.ARGB32.green(flashColour),
                        FastColor.ARGB32.blue(flashColour), alpha);
    }
}
