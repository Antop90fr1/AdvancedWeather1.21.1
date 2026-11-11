package net.antopfr.advancedweather.compat.catnip;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = "advancedweather", value = Dist.CLIENT)
public final class PlainOutliner {

    private PlainOutliner() {}

    private record Box(AABB box, float r, float g, float b, long expiry) {}

    private static final List<Box> BOXES = new ArrayList<>();

    static void show(List<BlockPos> valid, List<BlockPos> invalid, int durationTicks) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        long expiry = level.getGameTime() + durationTicks;
        for (BlockPos pos : valid) {
            BOXES.add(new Box(new AABB(pos), 0.29f, 0.88f, 0.35f, expiry)); // green
        }
        for (BlockPos pos : invalid) {
            BOXES.add(new Box(new AABB(pos), 0.88f, 0.63f, 0.19f, expiry)); // amber
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        if (BOXES.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            BOXES.clear();
            return;
        }

        long now = mc.level.getGameTime();
        BOXES.removeIf(b -> now >= b.expiry());
        if (BOXES.isEmpty()) return;

        Camera camera = event.getCamera();
        Vec3 cam = camera.getPosition();
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);

        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer consumer = buffers.getBuffer(RenderType.lines());
        for (Box b : BOXES) {
            LevelRenderer.renderLineBox(pose, consumer, b.box(), b.r(), b.g(), b.b(), 1.0f);
        }
        buffers.endBatch(RenderType.lines());
        pose.popPose();
    }
}
