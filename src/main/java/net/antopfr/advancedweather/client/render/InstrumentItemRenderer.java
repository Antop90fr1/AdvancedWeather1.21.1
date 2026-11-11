package net.antopfr.advancedweather.client.render;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.antopfr.advancedweather.AdvancedWeather;
import net.antopfr.advancedweather.client.state.ClientAtmosphereState;
import net.antopfr.advancedweather.client.wind.ClientWind;
import net.antopfr.advancedweather.weather.wind.WindScale;
import net.antopfr.advancedweather.content.item.AWItems;
import net.antopfr.advancedweather.compat.catnip.CatnipCompat;
import net.antopfr.advancedweather.weather.AtmosphericSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.ClientHooks;
import org.jetbrains.annotations.NotNull;

public class InstrumentItemRenderer extends BlockEntityWithoutLevelRenderer {

    public static final ModelResourceLocation BAROMETER_BODY = mrl("barometer_body");
    public static final ModelResourceLocation BAROMETER_NEEDLE = mrl("barometer_needle");
    public static final ModelResourceLocation THERMOMETER_BODY = mrl("thermometer_body");
//    public static final ModelResourceLocation THERMOMETER_COLUMN = mrl("thermometer_column");
    public static final ModelResourceLocation THERMOMETER_NEEDLE = mrl("thermometer_needle");
    public static final ModelResourceLocation HYGROMETER_BODY = mrl("hygrometer_body");
    public static final ModelResourceLocation HYGROMETER_NEEDLE = mrl("hygrometer_needle");
    public static final ModelResourceLocation ANEMOMETER_BODY = mrl("anemometer_body");
    public static final ModelResourceLocation ANEMOMETER_CUPS = mrl("anemometer_cups");

    private static ModelResourceLocation mrl(String name) {
        return ModelResourceLocation.standalone(
                ResourceLocation.fromNamespaceAndPath(AdvancedWeather.MOD_ID, "item/" + name));
    }

    private float smoothedPressure = 0.5f;
    private float smoothedTemp = 0.5f;
    private float smoothedHumidity = 0.5f;
    private float spinAngle = 0f;
    private long lastFrameTime = -1;

    public InstrumentItemRenderer(BlockEntityRenderDispatcher dispatcher, EntityModelSet modelSet) {
        super(dispatcher, modelSet);
    }

    @Override
    public void renderByItem(@NotNull ItemStack stack, @NotNull ItemDisplayContext context, @NotNull PoseStack pose,
                             @NotNull MultiBufferSource buffers, int light, int overlay) {
        updateSmoothing();

        boolean isGui = context == ItemDisplayContext.GUI;
        if (isGui) {
            Lighting.setupForFlatItems();
        }

        if (stack.is(AWItems.PORTABLE_BAROMETER.get())) {
            renderBody(BAROMETER_BODY, stack, context, pose, buffers, light, overlay);
            float angle = Mth.lerp(smoothedPressure, 135f, -135f);
            renderMobile(BAROMETER_NEEDLE, angle, Axis.ZP, stack, context, pose, buffers, light, overlay);

        } else if (stack.is(AWItems.PORTABLE_HYGROMETER.get())) {
            renderBody(HYGROMETER_BODY, stack, context, pose, buffers, light, overlay);
            float angle = Mth.lerp(smoothedHumidity, 135f, -135f);
            renderMobile(HYGROMETER_NEEDLE, angle, Axis.ZP, stack, context, pose, buffers, light, overlay);

//        } else if (stack.is(AWItems.PORTABLE_THERMOMETER.get())) {
//            renderBody(THERMOMETER_BODY, stack, context, pose, buffers, light, overlay);
//            renderColumn(THERMOMETER_COLUMN, smoothedTemp, stack, context, pose, buffers, light, overlay);

        } else if (stack.is(AWItems.PORTABLE_THERMOMETER.get())) {
            renderBody(THERMOMETER_BODY, stack, context, pose, buffers, light, overlay);
            float angle = Mth.lerp(smoothedTemp, 135f, -135f);
            renderMobile(THERMOMETER_NEEDLE, angle, Axis.ZP, stack, context, pose, buffers, light, overlay);

        } else if (stack.is(AWItems.PORTABLE_ANEMOMETER.get())) {
            renderBody(ANEMOMETER_BODY, stack, context, pose, buffers, light, overlay);
            renderMobile(ANEMOMETER_CUPS, spinAngle, Axis.YN, stack, context, pose, buffers, light, overlay);
        }

        if (isGui) {
            if (buffers instanceof MultiBufferSource.BufferSource src) {
                src.endBatch();
            }
            Lighting.setupFor3DItems();
        }
    }

    private void updateSmoothing() {
        long now = CatnipCompat.animationTicks();
        if (now == lastFrameTime) return;
        lastFrameTime = now;

        float targetP = Mth.clamp((ClientAtmosphereState.getLocalPressure() - AtmosphericSystem.P_MIN)
                / (AtmosphericSystem.P_MAX - AtmosphericSystem.P_MIN), 0f, 1f);
        float targetT = Mth.clamp((ClientAtmosphereState.getLocalTemperature() + 30f) / 80f, 0f, 1f);
        float targetH = Mth.clamp(ClientAtmosphereState.getLocalHumidity() / 100f, 0f, 1f);

        smoothedPressure = Mth.lerp(0.08f, smoothedPressure, targetP);
        smoothedTemp = Mth.lerp(0.08f, smoothedTemp, targetT);
        smoothedHumidity = Mth.lerp(0.08f, smoothedHumidity, targetH);

        float wind = Math.min(1f, WindScale.kmhToIntensity(ClientWind.localKmh()));
        spinAngle = (spinAngle + 2f + wind * 22f) % 360f;
    }

    private void renderBody(ModelResourceLocation model, ItemStack stack, ItemDisplayContext context,
                            PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        BakedModel baked = Minecraft.getInstance().getModelManager().getModel(model);
        pose.pushPose();
        pose.translate(0.5f, 0.5f, 0.5f);
        baked = ClientHooks.handleCameraTransforms(pose, baked, context, false);
        pose.translate(-0.5f, -0.5f, -0.5f);
        Minecraft.getInstance().getItemRenderer().renderModelLists(baked, stack, light, overlay, pose,
                buffers.getBuffer(Sheets.cutoutBlockSheet()));
        pose.popPose();
    }

    private void renderMobile(ModelResourceLocation model, float degrees, Axis axis,
                              ItemStack stack, ItemDisplayContext context,
                              PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        BakedModel baked = Minecraft.getInstance().getModelManager().getModel(model);
        pose.pushPose();
        pose.translate(0.5f, 0.5f, 0.5f);
        baked = ClientHooks.handleCameraTransforms(pose, baked, context, false);
        pose.mulPose(axis.rotationDegrees(degrees));
        pose.translate(-0.5f, -0.5f, -0.5f);
        Minecraft.getInstance().getItemRenderer().renderModelLists(baked, stack, light, overlay, pose,
                buffers.getBuffer(Sheets.cutoutBlockSheet()));
        pose.popPose();
    }

    private void renderColumn(ModelResourceLocation model, float fill,
                              ItemStack stack, ItemDisplayContext context,
                              PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        BakedModel baked = Minecraft.getInstance().getModelManager().getModel(model);
        pose.pushPose();
        pose.translate(0.5f, 0.5f, 0.5f);
        baked = ClientHooks.handleCameraTransforms(pose, baked, context, false);
        float baseY = 3f / 16f;
        pose.translate(-0.5f, baseY - 0.5f, -0.5f);
        pose.scale(1f, Math.max(0.05f, fill), 1f);
        pose.translate(0f, -baseY, 0f);
        Minecraft.getInstance().getItemRenderer().renderModelLists(baked, stack, light, overlay, pose,
                buffers.getBuffer(Sheets.cutoutBlockSheet()));
        pose.popPose();
    }
}