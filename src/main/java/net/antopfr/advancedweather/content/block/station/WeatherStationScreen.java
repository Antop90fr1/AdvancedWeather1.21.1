package net.antopfr.advancedweather.content.block.station;

import foundry.veil.api.client.render.VeilRenderSystem;
import net.antopfr.advancedweather.AdvancedWeather;
import net.antopfr.advancedweather.client.gui.StationBackdropRenderType;
import net.antopfr.advancedweather.client.state.ClientAtmosphereState;
import net.antopfr.advancedweather.client.state.ClientWeatherState;
import net.antopfr.advancedweather.config.AWClientConfig;
import net.antopfr.advancedweather.content.block.sensor.IWeatherSensor;
import net.antopfr.advancedweather.network.toserver.SetStationNamePacket;
import net.antopfr.advancedweather.util.Key;
import net.antopfr.advancedweather.util.ValueColors;
import net.antopfr.advancedweather.weather.WeatherTypes;
import net.antopfr.advancedweather.weather.FeelsLikeCalculation;
import net.antopfr.advancedweather.client.wind.ClientWind;
import net.antopfr.advancedweather.weather.wind.WindScale;
import net.antopfr.advancedweather.compat.catnip.CatnipCompat;
import net.antopfr.advancedweather.compat.catnip.Smoothed;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Objects;

public class WeatherStationScreen extends Screen {

    private static final int PANEL_W = 240;
    private static final int PANEL_H = 130;

    private float tintR = 0.3f, tintG = 0.35f, tintB = 0.45f;
    private float energy = 0f;

    private float cloudAmount = 0.5f;
    private float rainLevel = 0f;
    private float snowAmount = 0f;
    private float hailAmount = 0f;
    private float windLines = 0f;

    private float shaderTicks = 0f;

    private float openProgress = 0f;

    private final Smoothed displayTemp     = CatnipCompat.smoothed();
    private final Smoothed displayPressure = CatnipCompat.smoothed();
    private final Smoothed displayHumidity = CatnipCompat.smoothed();
    private final Smoothed displayWind     = CatnipCompat.smoothed();

    private boolean firstTick = true;

    private final WeatherStationBlockEntity station;
    private EditBox nameBox;
    private boolean editingName = false;

    public WeatherStationScreen(WeatherStationBlockEntity station) {
        super(Component.translatable("block.advancedweather.weather_station"));
        this.station = station;
    }

    @Override
    protected void init() {
        super.init();

        nameBox = new EditBox(font,
                (width - PANEL_W) / 2 + 40, (height - PANEL_H) / 2 + 4, PANEL_W - 80, 12,
                Component.translatable("advancedweather.station.name"));
        nameBox.setMaxLength(32);
        nameBox.setValue(station.getStationName());
        nameBox.setVisible(false);
        addRenderableWidget(nameBox);

        openProgress = 0f;
        firstTick = true;
    }

    @Override
    public void tick() {
        super.tick();

        shaderTicks += 1f;

        if (firstTick) {
            displayTemp.startWithValue(0f);
            displayPressure.startWithValue(1000f);
            displayHumidity.startWithValue(0f);
            displayWind.startWithValue(0f);
            firstTick = false;
        }

        displayTemp.chase(ClientAtmosphereState.getLocalTemperature(), 0.25);
        displayPressure.chase(ClientAtmosphereState.getLocalPressure(), 0.25);
        displayHumidity.chase(ClientAtmosphereState.getLocalHumidity(), 0.25);
        displayWind.chase(ClientWind.regionalKmh(), 0.25);

        displayTemp.tickChaser();
        displayPressure.tickChaser();
        displayHumidity.tickChaser();
        displayWind.tickChaser();

        float[] target = tintFor(ClientWeatherState.getCurrentWeather());
        tintR = Mth.lerp(0.06f, tintR, target[0]);
        tintG = Mth.lerp(0.06f, tintG, target[1]);
        tintB = Mth.lerp(0.06f, tintB, target[2]);
        energy = Mth.lerp(0.06f, energy, target[3]);
        cloudAmount = Mth.lerp(0.06f, cloudAmount, target[4]);

        Minecraft mc = Minecraft.getInstance();

        WeatherTypes cur = ClientWeatherState.getCurrentWeather();
        boolean snowy = cur == WeatherTypes.SNOW || cur == WeatherTypes.BLIZZARD;
        boolean hail = cur == WeatherTypes.HAIL;

        float targetSnow = switch (cur) {
            case BLIZZARD -> 1.0f;
            case SNOW -> 0.6f;
            default -> 0f;
        };
        snowAmount = Mth.lerp(0.06f, snowAmount, targetSnow);

        float targetHail = hail ? 0.8f : 0f;
        hailAmount = Mth.lerp(0.06f, hailAmount, targetHail);

        float targetRain = (!snowy && !hail && mc.level != null) ? mc.level.getRainLevel(1.0f) : 0f;
        rainLevel = Mth.lerp(0.1f, rainLevel, targetRain);

        float wi = ClientAtmosphereState.getWindIntensity();
        float targetWindLines = switch (cur) {
            case WINDY -> 0.9f;
            case BLIZZARD, SANDSTORM -> 0.7f;
            default -> wi > 0.5f ? (wi - 0.5f) * 1.2f : 0f;
        };
        windLines = Mth.lerp(0.06f, windLines, targetWindLines);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        openProgress = Math.min(1f, openProgress + partialTick * 0.12f);
        float ease = 1f - (1f - openProgress) * (1f - openProgress);

        g.fill(0, 0, this.width, this.height, ((int) (ease * 0xB0)) << 24);
        super.render(g, mouseX, mouseY, partialTick);

        int x = (width - PANEL_W) / 2;
        int targetY = (height - PANEL_H) / 2;
        int y = (int) Mth.lerp(ease, targetY + 40, targetY);

        int panelAlpha = (int) (ease * 0xE0);
        int textAlpha = Math.max(4, (int) (ease * 0xFF));

        float time = (shaderTicks + partialTick) * 0.04f;
        pushBackdropUniforms(time);
        drawBackdropQuad(g, x, y, PANEL_W, PANEL_H, panelAlpha);

        g.fill(x, y, x + PANEL_W, y + 18, ((int) (ease * 0xFF) << 24) | 0x1E2630);
        if (!editingName) {
            String title = station.getStationName().isBlank()
                    ? Key.t("block.advancedweather.weather_station") : station.getStationName();
            g.drawCenteredString(font, "§l" + title, x + PANEL_W / 2, y + 5,
                    withAlpha(0xFFFFFF, textAlpha));
            g.drawString(font, "✎", x + PANEL_W - 14, y + 5, withAlpha(0x777777, textAlpha), false);
        }
        nameBox.setX(x + 40);
        nameBox.setY(y + 4);

        AWClientConfig config = AWClientConfig.get();
        WeatherTypes current = ClientWeatherState.getCurrentWeather();

        int line = y + 26;
        int left = x + 10;
        int right = x + PANEL_W - 10;

        drawWeatherIcon(g, current, left, line, 16);
        g.drawString(font, current.displayString(), left + 22, line + 4, withAlpha(0xFFFFFF, textAlpha), true);
        line += 24;

        g.fill(left, line, right, line + 1, ((int) (ease * 0x30)) << 24 | 0xFFFFFF);
        line += 8;

        // ── Temperature — gated THERMOMETER ──
        var thermoStatus = station.getSensor(IWeatherSensor.SensorType.THERMOMETER);
        if (!station.sensorAvailable(IWeatherSensor.SensorType.THERMOMETER)) {
            drawStat(g, left, right, line, Key.t("advancedweather.temperature"), "-", 0x555555, "[" + Key.t("advancedweather.station.no_sensor") + "]", textAlpha);
        } else {
            float temp = displayTemp.getValue(partialTick);
            float realHum = ClientAtmosphereState.getLocalHumidity();
            float realWind = ClientWind.regionalKmh();
            float feelsLike = FeelsLikeCalculation.calculate(
                    ClientAtmosphereState.getLocalTemperature(), realWind, realHum);
            String comfort = FeelsLikeCalculation.getComfortLabel(feelsLike);

            float displayT = config.useFahrenheit ? temp * 1.8f + 32f : temp;
            String tUnit = config.useFahrenheit ? "°F" : "°C";

            if (thermoStatus != null && !thermoStatus.valid()) {
                drawStat(g, left, right, line, Key.t("advancedweather.temperature"),
                        String.format("%.1f %s", displayT, tUnit),
                        0xCC002F, "[" + Key.t("advancedweather.sensor.placement.check") + "]", textAlpha);
            } else {
                drawStat(g, left, right, line, Key.t("advancedweather.temperature"),
                        String.format("%.1f %s", displayT, tUnit),
                        ValueColors.temperature(temp), "[" + comfort + "]", textAlpha);
            }
        }
        line += 12;

        // ── Pressure — gated BAROMETER ──
        var baroStatus = station.getSensor(IWeatherSensor.SensorType.BAROMETER);
        if (!station.sensorAvailable(IWeatherSensor.SensorType.BAROMETER)) {
            drawStat(g, left, right, line, Key.t("advancedweather.pressure"), "-", 0x555555, "[" + Key.t("advancedweather.station.no_sensor") + "]", textAlpha);
        } else {
            float pressure = displayPressure.getValue(partialTick);
            float trend = ClientAtmosphereState.getTrend() * 20 * 60;
            String trendSym = trend > 0.002f ? "§a↑" : trend < -0.002f ? "§c↓" : "§7→";

            if (baroStatus != null && !baroStatus.valid()) {
                drawStat(g, left, right, line, Key.t("advancedweather.pressure"),
                        String.format("%.1f hPa", pressure),
                        0xCC002F, "[" + Key.t("advancedweather.sensor.placement.check") + "]", textAlpha);
            } else {
                drawStat(g, left, right, line, Key.t("advancedweather.pressure"),
                        String.format("%.1f hPa", pressure),
                        0xFFFFFF, "(" + trendSym + "§8)", textAlpha);
            }
        }
        line += 12;

        // ── Humidity — gated HYGROMETER ──
        var hygroStatus = station.getSensor(IWeatherSensor.SensorType.HYGROMETER);
        if (!station.sensorAvailable(IWeatherSensor.SensorType.HYGROMETER)) {
            drawStat(g, left, right, line, Key.t("advancedweather.humidity"), "-", 0x555555, "[" + Key.t("advancedweather.station.no_sensor") + "]", textAlpha);
        } else {
            float hum = displayHumidity.getValue(partialTick);
            if (hygroStatus != null && !hygroStatus.valid()) {
                drawStat(g, left, right, line, Key.t("advancedweather.humidity"),
                        String.format("%.0f%%", hum),
                        0xCC002F, "[" + Key.t("advancedweather.sensor.placement.check") + "]", textAlpha);
            } else {
                drawStat(g, left, right, line, Key.t("advancedweather.humidity"),
                        String.format("%.0f%%", hum),
                        ValueColors.humidity(hum), "[" + ValueColors.humidityLabel(hum) + "]", textAlpha);
            }
        }
        line += 12;

        // ── Wind — gated ANEMOMETER ──
        var anemoStatus = station.getSensor(IWeatherSensor.SensorType.ANEMOMETER);
        if (!station.sensorAvailable(IWeatherSensor.SensorType.ANEMOMETER)) {
            drawStat(g, left, right, line, Key.t("advancedweather.wind"), "-", 0x555555, "[" + Key.t("advancedweather.station.no_sensor") + "]", textAlpha);
        } else {
            float windKmh = displayWind.getValue(partialTick);
            String beaufort = WindScale.beaufort(windKmh).getString();
            float displayW = config.useMph ? windKmh * 0.621371f : windKmh;
            String wUnit = config.useMph ? "mph" : "km/h";

            if (anemoStatus != null && !anemoStatus.valid()) {
                drawStat(g, left, right, line, Key.t("advancedweather.wind"),
                        String.format("%.1f %s", displayW, wUnit),
                        0xCC002F, "[" + Key.t("advancedweather.sensor.placement.check") + "]", textAlpha);
            } else {
                drawStat(g, left, right, line, Key.t("advancedweather.wind"),
                        String.format("%.1f %s", displayW, wUnit),
                        ValueColors.wind(windKmh), "[" + beaufort + "]", textAlpha);
            }
        }
    }

    private void drawStat(GuiGraphics g, int left, int right, int y,
                          String label, String value, int valueColor, String suffix, int alpha) {
        g.drawString(font, label, left, y, withAlpha(0xAAAAAA, alpha), true);

        String suffixStr = "§8" + suffix;
        int suffixW = font.width(suffixStr);
        int valueW = font.width(value);

        g.drawString(font, suffixStr, right - suffixW, y, withAlpha(0x888888, alpha), true);
        g.drawString(font, value, right - suffixW - 4 - valueW, y, withAlpha(valueColor, alpha), true);
    }

    private void drawWeatherIcon(GuiGraphics g, WeatherTypes type, int x, int y, int size) {
        ResourceLocation icon = ResourceLocation.fromNamespaceAndPath(AdvancedWeather.MOD_ID,
                "textures/gui/weather_icons/" + type.name().toLowerCase() + ".png");
        g.blit(icon, x, y, 0, 0, size, size, size, size);
    }

    private static float[] tintFor(WeatherTypes type) {
        return switch (type) {
            // OVERWORLD
            case SUNNY, CLEAR, WINDY   -> new float[]{0.40f, 0.65f, 1.00f, 0.05f, 0.0f};
            case CLOUDY                -> new float[]{0.65f, 0.65f, 0.68f, 0.15f, 0.6f};  // ~overcast 0xA6A6AD
            case OVERCAST              -> new float[]{0.65f, 0.65f, 0.68f, 0.15f, 1.0f};  // 0xA6A6AD
            case MIST                  -> new float[]{0.78f, 0.80f, 0.82f, 0.10f, 0.9f};  // 0xC8CDD0
            case FOG                   -> new float[]{0.75f, 0.75f, 0.75f, 0.10f, 0.95f}; // 0xBFBFBF
            case DENSE_FOG             -> new float[]{0.65f, 0.65f, 0.68f, 0.10f, 0.95f}; // 0xA6A6AD
            case DRIZZLE, LIGHT_RAIN   -> new float[]{0.25f, 0.50f, 0.85f, 0.30f, 0.8f};
            case HEAVY_RAIN            -> new float[]{0.15f, 0.35f, 0.75f, 0.55f, 1.0f};
            case FREEZING_RAIN         -> new float[]{0.70f, 0.80f, 0.90f, 0.45f, 0.9f};  // 0xB3CCE6
            case THUNDERSTORM          -> new float[]{0.35f, 0.38f, 0.45f, 1.0f,  1.0f};  // 0x596173
            case SNOW                  -> new float[]{0.80f, 0.85f, 1.00f, 0.20f, 0.8f};
            case BLIZZARD              -> new float[]{0.88f, 0.88f, 0.88f, 0.75f, 1.0f};  // 0xE0E0E0
            case HAIL                  -> new float[]{0.81f, 0.81f, 0.67f, 0.65f, 0.9f};  // 0xCECFAB
            case SANDSTORM             -> new float[]{0.85f, 0.69f, 0.36f, 0.70f, 1.0f};  // 0xD9B05C
            // NETHER
            case ASH_STORM             -> new float[]{0.33f, 0.33f, 0.36f, 0.75f, 1.0f};  // 0x54545C
            case BRIMSTONE_STORM       -> new float[]{0.53f, 0.27f, 0.13f, 0.75f, 1.0f};  // 0x884422
            case LAVA_RAIN             -> new float[]{0.67f, 0.27f, 0.00f, 0.60f, 0.9f};  // 0xAA4400
            case NETHERSTORM           -> new float[]{0.40f, 0.13f, 0.00f, 1.0f,  1.0f};  // 0x662200
            case HELLFIRE              -> new float[]{0.80f, 0.20f, 0.00f, 1.0f,  1.0f};  // 0xCC3300
            case NETHER_CLEAR          -> new float[]{0.55f, 0.30f, 0.25f, 0.15f, 0.3f};
            // END
            case VOID_STORM            -> new float[]{0.10f, 0.02f, 0.20f, 0.85f, 1.0f};  // 0x05000A éclairci
            case END_MIST              -> new float[]{0.30f, 0.13f, 0.37f, 0.10f, 0.9f};  // 0x4D225E
            case CHORUS_GALE           -> new float[]{0.36f, 0.11f, 0.49f, 0.50f, 0.7f};  // 0x3A0B4E éclairci
            case ENDERSTORM            -> new float[]{0.20f, 0.05f, 0.35f, 0.85f, 1.0f};  // 0x16002A éclairci
            case END_CLEAR             -> new float[]{0.45f, 0.35f, 0.55f, 0.05f, 0.1f};
            default                    -> new float[]{0.40f, 0.45f, 0.60f, 0.20f, 0.5f};
        };
    }

    private void pushBackdropUniforms(float time) {
        var program = VeilRenderSystem.renderer().getShaderManager().getShader(
                ResourceLocation.fromNamespaceAndPath("advancedweather", "station_backdrop"));
        if (program != null) {
            Objects.requireNonNull(program.getUniform("AWTime")).setFloat(time);
            Objects.requireNonNull(program.getUniform("PanelAspect")).setFloat((float) PANEL_W / PANEL_H);
            Objects.requireNonNull(program.getUniform("WeatherTint")).setVector(tintR, tintG, tintB);
            Objects.requireNonNull(program.getUniform("WeatherEnergy")).setFloat(energy);
            Objects.requireNonNull(program.getUniform("CloudAmount")).setFloat(cloudAmount);
            Objects.requireNonNull(program.getUniform("RainLevel")).setFloat(rainLevel);
            Objects.requireNonNull(program.getUniform("SnowAmount")).setFloat(snowAmount);
            Objects.requireNonNull(program.getUniform("HailAmount")).setFloat(hailAmount);
            Objects.requireNonNull(program.getUniform("WindLines")).setFloat(windLines);
        }
    }

    private void drawBackdropQuad(GuiGraphics g, int x, int y, int w, int h, int alpha) {
        var consumer = g.bufferSource().getBuffer(StationBackdropRenderType.backdrop());
        var pose = g.pose().last();
        int color = (Mth.clamp(alpha, 0, 255) << 24) | 0x00FFFFFF;

        consumer.addVertex(pose, x,     y,     0).setUv(0f, 0f).setColor(color);
        consumer.addVertex(pose, x,     y + h, 0).setUv(0f, 1f).setColor(color);
        consumer.addVertex(pose, x + w, y + h, 0).setUv(1f, 1f).setColor(color);
        consumer.addVertex(pose, x + w, y,     0).setUv(1f, 0f).setColor(color);

        g.flush();
    }

    private static int withAlpha(int rgb, int alpha) {
        return (Mth.clamp(alpha, 0, 255) << 24) | (rgb & 0x00FFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = (width - PANEL_W) / 2;
        int y = (height - PANEL_H) / 2;
        if (!editingName && button == 0
                && mouseX >= x && mouseX < x + PANEL_W && mouseY >= y && mouseY < y + 18) {
            editingName = true;
            nameBox.setVisible(true);
            nameBox.setValue(station.getStationName());
            setFocused(nameBox);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (editingName && keyCode == 257) { // ENTER
            commitName();
            return true;
        }
        if (editingName && keyCode == 256) { // ESC
            editingName = false;
            nameBox.setVisible(false);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void commitName() {
        editingName = false;
        nameBox.setVisible(false);
        PacketDistributor.sendToServer(
                new SetStationNamePacket(
                        station.getBlockPos(), nameBox.getValue().trim()));
    }
}
