package net.antopfr.advancedweather.weather.effect.global.ripples;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.antopfr.advancedweather.client.state.ClientWeatherState;
import net.antopfr.advancedweather.util.PlayerChecks;
import net.antopfr.advancedweather.weather.WeatherTypes;
import net.antopfr.advancedweather.weather.effect.types.lava_rain.LavaRainSky;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

@EventBusSubscriber(modid = "advancedweather", value = Dist.CLIENT)
public class RippleEffect {

    private static final float SPAWN_RADIUS = 16f;
    private static final int   MAX_RIPPLES  = 80;
    private static final int   RIPPLE_LIFETIME = 16;
    private static final float MIN_RAIN_LEVEL = 0.05f;
    private static final int   LAVA_RIPPLE_COLOR = 0xFF7A2A;
    private static final Random random = new Random();

    private static final List<Ripple> ripples = new ArrayList<>();

    private static class Ripple {
        Vec3 pos;
        int age = 0;
        float maxSize;
        int color;

        Ripple(Vec3 pos, float maxSize) {
            this(pos, maxSize, 0xFFFFFF);
        }

        Ripple(Vec3 pos, float maxSize, int color) {
            this.pos = pos;
            this.maxSize = maxSize;
            this.color = color;
        }
    }

    private record Impact(int y, boolean lava) {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.level instanceof ClientLevel level) || mc.player == null) {
            ripples.clear();
            return;
        }

        if (mc.isPaused()) return;

        float rainLevel = level.getRainLevel(1.0f);

        if (rainLevel > MIN_RAIN_LEVEL) {
            int spawnAttempts = Math.round(Mth.lerp(rainLevel, 0.5f, 6f));

            Vec3 playerPos = mc.player.position();

            while (spawnAttempts-- > 0 && ripples.size() < MAX_RIPPLES) {
                double angle = random.nextDouble() * Math.PI * 2;
                double dist = random.nextDouble() * SPAWN_RADIUS;
                double x = playerPos.x + Math.cos(angle) * dist;
                double z = playerPos.z + Math.sin(angle) * dist;

                BlockPos columnPos = new BlockPos((int) Math.floor(x), (int) playerPos.y, (int) Math.floor(z));
                BlockPos waterSurface = findWaterSurface(mc, columnPos);
                if (waterSurface == null) continue;

                Vec3 ripplePos = new Vec3(x, waterSurface.getY() + 1.001, z);
                float maxSize = 0.5f + random.nextFloat() * 0.4f;
                ripples.add(new Ripple(ripplePos, maxSize));
            }
        }

        WeatherTypes weather = ClientWeatherState.getCurrentWeather();
        if ((weather == WeatherTypes.LAVA_RAIN || weather == WeatherTypes.NETHERSTORM)
                && !PlayerChecks.isShielded(level, mc.player.blockPosition())) {
            Vec3 pp = mc.player.position();
            for (int attempt = 0; attempt < 10; attempt++) {
                double angle = random.nextDouble() * Math.PI * 2;
                double dist = random.nextDouble() * SPAWN_RADIUS;
                double x = pp.x + Math.cos(angle) * dist;
                double z = pp.z + Math.sin(angle) * dist;

                int blockX = (int) Math.floor(x);
                int blockZ = (int) Math.floor(z);
                Impact hit = findLavaRainImpact(level, blockX, (int) Math.floor(pp.y), blockZ);
                if (hit == null) continue;

                LavaRainSky.Opening opening = LavaRainSky.below(level, blockX, hit.y() + 1.5, blockZ);
                if (opening == null || opening.floor() != hit.y()) continue;

                if (hit.lava()) {
                    if (ripples.size() < MAX_RIPPLES) {
                        float maxSize = 0.5f + random.nextFloat() * 0.4f;
                        ripples.add(new Ripple(new Vec3(x, hit.y() + 1.001, z), maxSize, LAVA_RIPPLE_COLOR));
                    }
                }
            }
        }

        Iterator<Ripple> it = ripples.iterator();
        while (it.hasNext()) {
            Ripple r = it.next();
            r.age++;
            if (r.age >= RIPPLE_LIFETIME) it.remove();
        }
    }

    private static BlockPos findWaterSurface(Minecraft mc, BlockPos near) {
        for (int dy = 3; dy >= -12; dy--) {
            BlockPos check = near.offset(0, dy, 0);
            FluidState fluid = mc.level.getFluidState(check);
            if (fluid.is(Fluids.WATER) && fluid.isSource()) {
                BlockPos above = check.above();
                if (mc.level.getFluidState(above).isEmpty() && mc.level.canSeeSky(above)) {
                    return check;
                }
            }
        }
        return null;
    }

    private static Impact findLavaRainImpact(ClientLevel level, int x, int startY, int z) {
        for (int y = startY + 3; y >= startY - 16; y--) {
            if (!level.getBlockState(new BlockPos(x, y + 1, z)).isAir()) continue;
            BlockPos p = new BlockPos(x, y, z);
            if (level.getFluidState(p).is(Fluids.LAVA)) {
                return new Impact(y, true);
            }
            BlockState s = level.getBlockState(p);
            if (!s.isAir() && level.getFluidState(p).isEmpty()) {
                return new Impact(y, false);
            }
        }
        return null;
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        if (ripples.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        Camera camera = event.getCamera();
        Vec3 camPos = camera.getPosition();

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(-camPos.x, -camPos.y, -camPos.z);

        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        VertexConsumer consumer = bufferSource.getBuffer(RippleRenderType.ripples());
        PoseStack.Pose pose = poseStack.last();

        for (Ripple r : ripples) {
            float progress = r.age / (float) RIPPLE_LIFETIME;
            float size = r.maxSize * progress;
            float alpha = (1f - progress) * 0.7f;
            int colorInt = (Mth.clamp((int)(alpha * 255), 0, 255) << 24) | (r.color & 0x00FFFFFF);

            drawRippleQuad(consumer, pose, (float) r.pos.x, (float) r.pos.y, (float) r.pos.z, size, colorInt);
        }

        bufferSource.endBatch(RippleRenderType.ripples());
        poseStack.popPose();
    }

    private static void drawRippleQuad(VertexConsumer consumer, PoseStack.Pose pose,
                                       float cx, float cy, float cz,
                                       float size, int color) {
        float half = size / 2f;

        consumer.addVertex(pose, cx - half, cy, cz - half).setUv(0f, 0f).setColor(color);
        consumer.addVertex(pose, cx - half, cy, cz + half).setUv(0f, 1f).setColor(color);
        consumer.addVertex(pose, cx + half, cy, cz + half).setUv(1f, 1f).setColor(color);

        consumer.addVertex(pose, cx - half, cy, cz - half).setUv(0f, 0f).setColor(color);
        consumer.addVertex(pose, cx + half, cy, cz + half).setUv(1f, 1f).setColor(color);
        consumer.addVertex(pose, cx + half, cy, cz - half).setUv(1f, 0f).setColor(color);
    }
}
