package net.antopfr.advancedweather.content.item.kite;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.antopfr.advancedweather.AdvancedWeather;
import net.antopfr.advancedweather.content.entity.KiteEntity;
import net.antopfr.advancedweather.weather.wind.WindField;
import net.antopfr.advancedweather.weather.wind.WindSample;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class KiteRenderer extends EntityRenderer<KiteEntity> {

    private static ResourceLocation tex(String name) {
        return ResourceLocation.fromNamespaceAndPath(
                AdvancedWeather.MOD_ID, "textures/entity/" + name + ".png");
    }

    private static final ResourceLocation BASE         = tex("kite");
    private static final ResourceLocation TOP_LEFT     = tex("kite_top_left");
    private static final ResourceLocation TOP_RIGHT    = tex("kite_top_right");
    private static final ResourceLocation BOTTOM_LEFT  = tex("kite_bottom_left");
    private static final ResourceLocation BOTTOM_RIGHT = tex("kite_bottom_right");

    private static final float HALF = 0.9f;

    private static final float BRIDLE_WIDTH = 0.009f;
    private static final float TETHER_WIDTH = 0.018f;

    private static float px(float p) { return HALF * (2f * p / 64f - 1f); }
    private static float py(float p) { return HALF * (1f - 2f * p / 64f); }

    private static final Vec3 A_TOP   = new Vec3(px(31.5f), py(0f),  0);
    private static final Vec3 A_LEFT  = new Vec3(px(6f),    py(17f), 0);
    private static final Vec3 A_RIGHT = new Vec3(px(57f),   py(17f), 0);
    private static final Vec3 A_TIP   = new Vec3(px(31.5f), py(55f), 0);

    private static final Vec3 BRIDLE = new Vec3(0, py(30f), 0.28);

    public KiteRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(KiteEntity kite, float yaw, float partialTick,
                       PoseStack pose, @NotNull MultiBufferSource buffers, int light) {

        Vec3 at = kite.getPosition(partialTick);
        WindSample air = WindField.sample(kite.level(), at.x, at.y, at.z, partialTick);
        Vec3 wind = air.direction();
        float windYaw = (float) Math.toDegrees(Math.atan2(wind.z, wind.x));
        float intensity = Math.min(1f, air.intensity());

        float rotY = -windYaw + 270f - 19f;
        float rotX = 70f - intensity * 20f;
        float rotZ = Mth.sin((kite.tickCount + partialTick) * 0.12f) * 5f;

        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(rotY));
        pose.mulPose(Axis.XP.rotationDegrees(rotX));
        pose.mulPose(Axis.ZP.rotationDegrees(rotZ));

        KiteColors c = kite.getColors();
        Matrix4f mat = pose.last().pose();

        quad(buffers, mat, BASE,         0xFFFFFF,        light);
        quad(buffers, mat, TOP_LEFT,     c.topLeft(),     light);
        quad(buffers, mat, TOP_RIGHT,    c.topRight(),    light);
        quad(buffers, mat, BOTTOM_LEFT,  c.bottomLeft(),  light);
        quad(buffers, mat, BOTTOM_RIGHT, c.bottomRight(), light);

        int kiteLight = LevelRenderer.getLightColor(kite.level(), kite.blockPosition());
        cord(buffers, mat, A_TOP,   BRIDLE, 0.0, BRIDLE_WIDTH, kiteLight, kiteLight, 2);
        cord(buffers, mat, A_LEFT,  BRIDLE, 0.0, BRIDLE_WIDTH, kiteLight, kiteLight, 2);
        cord(buffers, mat, A_RIGHT, BRIDLE, 0.0, BRIDLE_WIDTH, kiteLight, kiteLight, 2);
        cord(buffers, mat, A_TIP,   BRIDLE, 0.0, BRIDLE_WIDTH, kiteLight, kiteLight, 2);

        pose.popPose();

        renderTether(kite, partialTick, pose, buffers, rotY, rotX, rotZ);
    }

    private void renderTether(KiteEntity kite, float partialTick, PoseStack pose,
                              MultiBufferSource buffers,
                              float rotY, float rotX, float rotZ) {
        Player owner = kite.getOwnerPlayer();
        if (owner == null) return;

        Vector3f b = new Vector3f((float) BRIDLE.x, (float) BRIDLE.y, (float) BRIDLE.z);
        new Quaternionf()
                .rotateY((float) Math.toRadians(rotY))
                .rotateX((float) Math.toRadians(rotX))
                .rotateZ((float) Math.toRadians(rotZ))
                .transform(b);
        Vec3 anchor = new Vec3(b.x, b.y, b.z);

        Vec3 kitePos = kite.getPosition(partialTick);
        Vec3 handPos = KiteHold.handPosition(owner, kite, partialTick);
        Vec3 target = anchor.add(handPos.subtract(kitePos.add(anchor)));

        int lightKite = LevelRenderer.getLightColor(kite.level(),
                BlockPos.containing(kitePos.add(anchor)));
        int lightHand = LevelRenderer.getLightColor(owner.level(),
                BlockPos.containing(handPos));

        double sag = owner.isFallFlying() ? -0.08 : -0.45;
        cord(buffers, pose.last().pose(), anchor, target,
                sag, TETHER_WIDTH, lightKite, lightHand, 24);
    }

    private static void cord(MultiBufferSource buffers, Matrix4f mat,
                             Vec3 from, Vec3 to, double sagAmount, float width,
                             int lightA, int lightB, int segments) {
        VertexConsumer vc = buffers.getBuffer(RenderType.leash());
        Vec3 rel = to.subtract(from);

        Vec3 dir = rel.normalize();
        Vec3 perpA = new Vec3(-dir.z, 0, dir.x).normalize().scale(width);
        Vec3 perpB = dir.cross(perpA).normalize().scale(width);

        for (int plane = 0; plane < 2; plane++) {
            Vec3 off = plane == 0 ? perpA : perpB;

            for (int i = 0; i <= segments; i++) {
                float f = i / (float) segments;
                double sag = Math.sin(f * Math.PI) * sagAmount;

                double x = from.x + rel.x * f;
                double y = from.y + rel.y * f + sag;
                double z = from.z + rel.z * f;

                float shade = (i % 2 == 0) ? 0.75f : 1.0f;
                float col = 0.85f * shade;
                int light = interpolateLight(lightA, lightB, f);

                vc.addVertex(mat, (float)(x - off.x), (float)(y - off.y), (float)(z - off.z))
                        .setColor(col, col, col * 0.94f, 1f)
                        .setLight(light);
                vc.addVertex(mat, (float)(x + off.x), (float)(y + off.y), (float)(z + off.z))
                        .setColor(col, col, col * 0.94f, 1f)
                        .setLight(light);
            }
        }
    }

    private static int interpolateLight(int a, int b, float f) {
        int blockA = a & 0xFFFF, skyA = (a >> 16) & 0xFFFF;
        int blockB = b & 0xFFFF, skyB = (b >> 16) & 0xFFFF;
        int block = (int) Mth.lerp(f, blockA, blockB);
        int sky = (int) Mth.lerp(f, skyA, skyB);
        return block | (sky << 16);
    }

    private static void quad(MultiBufferSource buffers, Matrix4f mat,
                             ResourceLocation texture, int rgb, int light) {
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(texture));
        int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;

        v(vc, mat, -HALF,  HALF, 0f, 0f, r, g, b, light);
        v(vc, mat, -HALF, -HALF, 0f, 1f, r, g, b, light);
        v(vc, mat,  HALF, -HALF, 1f, 1f, r, g, b, light);
        v(vc, mat,  HALF,  HALF, 1f, 0f, r, g, b, light);

        v(vc, mat,  HALF,  HALF, 1f, 0f, r, g, b, light);
        v(vc, mat,  HALF, -HALF, 1f, 1f, r, g, b, light);
        v(vc, mat, -HALF, -HALF, 0f, 1f, r, g, b, light);
        v(vc, mat, -HALF,  HALF, 0f, 0f, r, g, b, light);
    }

    private static void v(VertexConsumer vc, Matrix4f mat, float x, float y,
                          float u, float vv, int r, int g, int b, int light) {
        vc.addVertex(mat, x, y, 0f)
                .setColor(r, g, b, 255)
                .setUv(u, vv)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(0f, 0f, 1f);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull KiteEntity entity) {
        return BASE;
    }
}
