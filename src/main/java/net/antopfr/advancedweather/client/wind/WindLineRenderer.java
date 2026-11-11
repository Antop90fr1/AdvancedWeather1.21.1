package net.antopfr.advancedweather.client.wind;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.antopfr.advancedweather.AdvancedWeather;
import net.antopfr.advancedweather.client.state.ClientWeatherState;
import net.antopfr.advancedweather.config.AWClientConfig;
import net.antopfr.advancedweather.weather.WeatherEffects;
import net.antopfr.advancedweather.weather.wind.WindField;
import net.antopfr.advancedweather.weather.wind.WindSample;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = AdvancedWeather.MOD_ID, value = Dist.CLIENT)
public final class WindLineRenderer {

    private WindLineRenderer() {}

    private static final int MAX_SPAWN_PER_TICK = 4;
    private static final double MIN_SPEED = 0.25;

    private static final List<WindLine> LINES = new ArrayList<>();
    private static final RandomSource RANDOM = RandomSource.create();

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.isPaused()) return;

        if (!windLinesActive()) {
            LINES.clear();
            return;
        }

        long now = level.getGameTime();
        LINES.removeIf(line -> line.expired(now));

        int target = (int) (AWClientConfig.get().windLineMax * ClientWind.visibility());
        Vec3 camera = mc.gameRenderer.getMainCamera().getPosition();
        for (int i = 0; i < MAX_SPAWN_PER_TICK && LINES.size() < target; i++) {
            spawn(level, camera, now);
        }
    }

    private static void spawn(ClientLevel level, Vec3 camera, long now) {
        AWClientConfig config = AWClientConfig.get();
        Vec3 prevailing = ClientWind.direction();

        double spawnDist = Mth.lerp(RANDOM.nextDouble(), config.windLineSpawnDistMin, config.windLineSpawnDistMax);
        double lateral = (RANDOM.nextDouble() - 0.5) * config.windLineLateralSpread;
        double vertical = (RANDOM.nextDouble() - 0.5) * config.windLineHeightSpread;

        Vec3 side = new Vec3(-prevailing.z, 0, prevailing.x);
        Vec3 start = camera
                .subtract(prevailing.scale(spawnDist))
                .add(side.scale(lateral))
                .add(0, vertical, 0);

        WindSample air = WindField.sample(level, start);
        double speed = Math.max(MIN_SPEED, air.blocksPerTick());
        int lifetime = Mth.clamp((int) (spawnDist * 2.0 / speed), 30, 160);
        float length = (float) Mth.lerp(RANDOM.nextDouble(), config.windLineLengthMin, config.windLineLengthMax);
        float opacity = 0.05f + RANDOM.nextFloat() * 0.3f;

        LINES.add(new WindLine(start, air.direction(), speed, length, opacity, now, lifetime));
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        if (LINES.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(true);
        double now = mc.level.getGameTime() + partialTick;
        Vec3 camera = event.getCamera().getPosition();
        float exposure = ClientWind.exposure();

        PoseStack poseStack = event.getPoseStack();
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();

        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer consumer = buffers.getBuffer(RenderType.LINES);

        for (WindLine line : LINES) {
            double age = now - line.birthTick();
            if (age < 0 || age > line.lifetime()) continue;

            float life = (float) (age / line.lifetime());
            float alpha = line.opacity() * exposure;
            if (life < 0.1f) alpha *= life / 0.1f;
            if (life > 0.8f) alpha *= (1f - life) / 0.2f;
            if (alpha <= 0.002f) continue;

            Vec3 dir = line.direction();
            Vec3 head = line.positionAt(age).subtract(camera);
            Vec3 tail = head.add(dir.scale(line.length()));

            consumer.addVertex(matrix, (float) head.x, (float) head.y, (float) head.z)
                    .setColor(1f, 1f, 1f, alpha)
                    .setNormal(pose, (float) dir.x, (float) dir.y, (float) dir.z);
            consumer.addVertex(matrix, (float) tail.x, (float) tail.y, (float) tail.z)
                    .setColor(1f, 1f, 1f, alpha)
                    .setNormal(pose, (float) dir.x, (float) dir.y, (float) dir.z);
        }

        buffers.endBatch(RenderType.LINES);
    }

    private static boolean windLinesActive() {
        return ClientWeatherState.hasEffect(WeatherEffects.WIND_LINES)
                || ClientWeatherState.hasEffect(WeatherEffects.NETHER_WIND_LINES)
                || ClientWeatherState.hasEffect(WeatherEffects.END_WIND_LINES);
    }
}
