package net.antopfr.advancedweather.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.antopfr.advancedweather.AdvancedWeather;
import net.antopfr.advancedweather.client.state.ClientAtmosphereState;
import net.antopfr.advancedweather.content.block.sensor.IWeatherSensor;
import net.antopfr.advancedweather.weather.AtmosphericSystem;
import net.antopfr.advancedweather.weather.wind.WindField;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class SensorBlockRenderer<T extends BlockEntity & IWeatherSensor> implements BlockEntityRenderer<T> {

    public static final ModelResourceLocation NEEDLE = mrl("needle");

    private static ModelResourceLocation mrl(String name) {
        return ModelResourceLocation.standalone(
                ResourceLocation.fromNamespaceAndPath(AdvancedWeather.MOD_ID, "block/" + name));
    }

    private static final float PIVOT_REL_X = (11f / 16f) - 0.5f;
    private static final float PIVOT_REL_Y = (5f / 16f) - 0.5f;
    private static final float PIVOT_REL_Z = (-0.5f / 16f) - 0.5f;

    public SensorBlockRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(T be, float partialTick, PoseStack pose, MultiBufferSource buffers,
                       int light, int overlay) {
        var level = be.getLevel();
        if (level == null) return;
        BlockPos pos = be.getBlockPos();

        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);

        var facing = be.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        pose.mulPose(Axis.YP.rotationDegrees(270f - facing.toYRot()));

        switch (be.getSensorType()) {
            case BAROMETER -> {
                float p = Mth.clamp((ClientAtmosphereState.getLocalPressureAt(level, pos)
                        - AtmosphericSystem.P_MIN)
                        / (AtmosphericSystem.P_MAX - AtmosphericSystem.P_MIN), 0f, 1f);
                renderNeedle(Mth.lerp(p, -135f, 135f), pose, buffers, light, overlay);
            }
            case THERMOMETER -> {
                float t = Mth.clamp((ClientAtmosphereState.getLocalTemperatureAt(level, pos) + 30f) / 80f,
                        0f, 1f);
                renderNeedle(Mth.lerp(t, -135f, 135f), pose, buffers, light, overlay);
            }
            case HYGROMETER -> {
                float h = Mth.clamp(ClientAtmosphereState.getLocalHumidityAt(level, pos) / 100f, 0f, 1f);
                renderNeedle(Mth.lerp(h, -135f, 135f), pose, buffers, light, overlay);
            }
            case ANEMOMETER -> {
                float wind = Math.min(1f, WindField.sample(level,
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, partialTick).intensity());
                renderNeedle(Mth.lerp(wind, -135f, 135f), pose, buffers, light, overlay);
            }
        }

        pose.popPose();
    }

    private void renderNeedle(float degrees, PoseStack pose,
                              MultiBufferSource buffers, int light, int overlay) {
        BakedModel baked = Minecraft.getInstance().getModelManager().getModel(NEEDLE);

        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-90f));
        pose.translate(PIVOT_REL_X, PIVOT_REL_Y, PIVOT_REL_Z);
        pose.mulPose(Axis.ZP.rotationDegrees(degrees));
        pose.translate(-PIVOT_REL_X, -PIVOT_REL_Y, -PIVOT_REL_Z);
        pose.translate(-0.5f, -0.5f, -0.5f);
        Minecraft.getInstance().getItemRenderer().renderModelLists(baked, ItemStack.EMPTY, light, overlay, pose, buffers.getBuffer(Sheets.cutoutBlockSheet()));
        pose.popPose();
    }
}
