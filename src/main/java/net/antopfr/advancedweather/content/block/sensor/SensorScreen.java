package net.antopfr.advancedweather.content.block.sensor;

import net.antopfr.advancedweather.client.state.ClientAtmosphereState;
import net.antopfr.advancedweather.util.Key;
import net.antopfr.advancedweather.util.UnitFormat;
import net.antopfr.advancedweather.util.ValueColors;
import net.antopfr.advancedweather.weather.wind.WindField;
import net.antopfr.advancedweather.weather.wind.WindScale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;

public class SensorScreen extends Screen {

    private static final int PANEL_W = 160;
    private static final int PANEL_H = 96;

    private final BlockEntity sensorBe;
    private final IWeatherSensor sensor;
    private float openProgress = 0f;

    public <T extends BlockEntity & IWeatherSensor> SensorScreen(T be) {
        super(Component.translatable("advancedweather.sensor.title"));
        this.sensorBe = be;
        this.sensor = be;
    }

    @Override
    public void render(@NotNull GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        openProgress = Math.min(1f, openProgress + partialTick * 0.15f);
        float ease = 1f - (1f - openProgress) * (1f - openProgress);

        g.fill(0, 0, width, height, ((int) (ease * 0x90)) << 24);
        super.render(g, mouseX, mouseY, partialTick);

        int x = (width - PANEL_W) / 2;
        int y = (int) Mth.lerp(ease, (height - PANEL_H) / 2f + 20, (height - PANEL_H) / 2f);
        int alpha = Math.max(4, (int) (ease * 0xFF));

        g.fill(x, y, x + PANEL_W, y + PANEL_H, ((int) (ease * 0xE0) << 24) | 0x101418);
        g.fill(x, y, x + PANEL_W, y + 16, (alpha << 24) | 0x1E2630);

        Level level = sensorBe.getLevel();
        BlockPos pos = sensorBe.getBlockPos();
        if (level == null) return;

        String title;
        String value;
        String sub;
        int valueColor;

        switch (sensor.getSensorType()) {
            case BAROMETER -> {
                title = Key.t("block.advancedweather.barometer");;
                float p = ClientAtmosphereState.getLocalPressureAt(level, pos);
                value = String.format("%.1f hPa", p);
                valueColor = 0xFFFFFF;
                float trend = ClientAtmosphereState.getTrend() * 20 * 60;
                sub = trend > 0.002f ? "§a↑ " + Key.t("advancedweather.trend.rising")
                        : trend < -0.002f ? "§c↓ " + Key.t("advancedweather.trend.falling")
                        : "§7→ " + Key.t("advancedweather.trend.steady");
            }
            case THERMOMETER -> {
                title = Key.t("block.advancedweather.thermometer");
                float t = ClientAtmosphereState.getLocalTemperatureAt(level, pos);
                value = UnitFormat.temperature(t);
                valueColor = ValueColors.temperature(t);
                sub = "§7" + Component.translatable("advancedweather.sensor.at_y", pos.getY()).getString();
            }
            case HYGROMETER -> {
                title = Key.t("block.advancedweather.hygrometer");;
                float h = ClientAtmosphereState.getLocalHumidityAt(level, pos);
                value = String.format("%.0f%%", h);
                valueColor = ValueColors.humidity(h);
                sub = "§7[" + ValueColors.humidityLabel(h) + "]";
            }
            case ANEMOMETER -> {
                title = Key.t("block.advancedweather.anemometer");;
                float kmh = WindField.sample(level, pos).speedKmh();
                value = UnitFormat.wind(kmh);
                valueColor = ValueColors.wind(kmh);
                sub = "§7[" + WindScale.beaufort(kmh).getString() + "]";
            }
            default -> { title = Key.t("advancedweather.sensor.title"); value = "—"; sub = ""; valueColor = 0x777777; }
        }

        g.drawCenteredString(font, "§l" + title, x + PANEL_W / 2, y + 4, withAlpha(0xFFFFFF, alpha));

        g.pose().pushPose();
        g.pose().translate(x + PANEL_W / 2f, y + 30, 0);
        g.pose().scale(2f, 2f, 1f);
        g.drawCenteredString(font, value, 0, 0, withAlpha(valueColor, alpha));
        g.pose().popPose();

        g.drawCenteredString(font, sub, x + PANEL_W / 2, y + 52, withAlpha(0xAAAAAA, alpha));

        boolean clientValidGuess = sensorPlacementHint();
        g.drawString(font, Key.label("advancedweather.sensor.placement") + (clientValidGuess
                        ? "§a✔ " + Key.t("advancedweather.sensor.placement.valid")
                        : "§6⚠ " + Key.t("advancedweather.sensor.placement.check")),
                x + 8, y + PANEL_H - 24, withAlpha(0xCCCCCC, alpha), true);
        g.drawString(font, "§8" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ(),
                x + 8, y + PANEL_H - 12, withAlpha(0x666666, alpha), true);
    }

    private boolean sensorPlacementHint() {
        Level level = sensorBe.getLevel();
        BlockPos pos = sensorBe.getBlockPos();
        if (!(level instanceof ClientLevel)) return true;
        if (level.dimension() != Level.OVERWORLD) return true;
        return switch (sensor.getSensorType()) {
            case BAROMETER -> true;
            case THERMOMETER -> !level.canSeeSky(pos.above()) && openHorizontalSides(level, pos) >= 2;
            case HYGROMETER -> level.canSeeSky(pos.above()) && noAdjacentFluids(level, pos);
            case ANEMOMETER -> level.canSeeSky(pos.above()) && openHorizontalSides(level, pos) == 4;
        };
    }

    private static int openHorizontalSides(Level level, BlockPos pos) {
        int open = 0;
        for (var dir : Direction.Plane.HORIZONTAL) {
            BlockPos side = pos.relative(dir);
            if (!level.getBlockState(side).isSolidRender(level, side)) open++;
        }
        return open;
    }

    private static boolean noAdjacentFluids(Level level, BlockPos pos) {
        for (var dir : Direction.values()) {
            if (!level.getFluidState(pos.relative(dir)).isEmpty()) return false;
        }
        return true;
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private static int withAlpha(int rgb, int alpha) {
        return (Mth.clamp(alpha, 0, 255) << 24) | (rgb & 0x00FFFFFF);
    }
}