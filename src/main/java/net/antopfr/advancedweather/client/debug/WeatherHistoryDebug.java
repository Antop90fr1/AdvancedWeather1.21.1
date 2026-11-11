package net.antopfr.advancedweather.client.debug;

import net.antopfr.advancedweather.client.ClientLocalHistory;
import net.antopfr.advancedweather.client.state.ClientAtmosphereState;
import net.antopfr.advancedweather.client.wind.ClientWind;
import net.antopfr.advancedweather.weather.wind.WindScale;
import net.antopfr.advancedweather.weather.WeatherTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.List;

import static net.antopfr.advancedweather.weather.wind.WindScale.MAX_KMH;

public class WeatherHistoryDebug extends Screen {

    private static final int MINUTES = 60;
    private static final int GRAPH_H       = 60;
    private static final int TEMP_GRAPH_H  = 80;
    private static final int HUM_GRAPH_H   = 60;
    private static final int WIND_GRAPH_H  = 60;
    private static final int BAR_H         = 16;
    private static final int PADDING       = 14;

    private ClientLocalHistory.LocalEntry hoveredEntry = null;
    private int hoveredX = 0;
    private int hoveredY = 0;
    private String hoveredValueText = "";
    private long hoveredTimeAgo = 0;

    public WeatherHistoryDebug() {
        super(Component.literal("Weather History"));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, 0xDD000000);
        super.render(g, mouseX, mouseY, partialTick);

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        List<ClientLocalHistory.LocalEntry> localEntries = ClientLocalHistory.getLastMinutes(MINUTES);

        if (localEntries.isEmpty()) {
            g.drawCenteredString(font, "No data yet", width / 2, height / 2, 0xAAAAAA);
            return;
        }

        // ── CALCUL DYNAMIQUE : Bornes de Pression ────────────────────────────
        float minP = localEntries.stream().map(ClientLocalHistory.LocalEntry::localPressure).reduce(Float.MAX_VALUE, Math::min) - 10f;
        float maxP = localEntries.stream().map(ClientLocalHistory.LocalEntry::localPressure).reduce(Float.MIN_VALUE, Math::max) + 10f;
        minP = (float)(Math.floor(minP / 10) * 10);
        maxP = (float)(Math.ceil(maxP  / 10) * 10);
        if (minP == maxP) maxP += 10f;

        // ── CALCUL DYNAMIQUE : Bornes de Température ─────────────────────────
        float minT = localEntries.stream().map(ClientLocalHistory.LocalEntry::localTemperature).reduce(Float.MAX_VALUE, Math::min) - 5f;
        float maxT = localEntries.stream().map(ClientLocalHistory.LocalEntry::localTemperature).reduce(Float.MIN_VALUE, Math::max) + 5f;

        minT = (float)(Math.floor(minT / 5) * 5);
        maxT = (float)(Math.ceil(maxT  / 5) * 5);
        if (minT == maxT) maxT += 5f;

        long currentTick = mc.level.getGameTime();
        hoveredEntry = null;

        int graphX = PADDING + 24;
        int graphW = width - graphX - PADDING;

        g.drawString(font, "§bWeather History §7- last " + MINUTES + " min in-game", PADDING, PADDING, 0xFFFFFF, false);

        // 1. Types de Météo
        int barY = PADDING + 12;
        renderWeatherBars(g, localEntries, graphX, barY, graphW, BAR_H, mouseX, mouseY, currentTick);
        g.drawString(font, "§7Weather type", PADDING, barY + BAR_H + 2, 0xAAAAAA, false);

        // 2. Courbe de Pression
        int pressureY = barY + BAR_H + 14;
        g.drawString(font, "§7Pressure (hPa)", PADDING, pressureY, 0xAAAAAA, false);
        pressureY += 10;
        renderPressureCurve(g, localEntries, graphX, pressureY, graphW, GRAPH_H, mouseX, mouseY, currentTick, minP, maxP);

        g.drawString(font, String.format("%.0f", maxP), PADDING, pressureY, 0x666666, false);
        g.drawString(font, String.format("%.0f", (minP + maxP) / 2), PADDING, pressureY + GRAPH_H / 2 - 4, 0x666666, false);
        g.drawString(font, String.format("%.0f", minP), PADDING, pressureY + GRAPH_H - 8, 0x666666, false);
        int nominalY = pressureY + (int)(Mth.clamp((1013f - minP) / (maxP - minP), 0f, 1f) * GRAPH_H);
        g.fill(graphX, nominalY, graphX + graphW, nominalY + 1, 0x22FFFFFF);

        // 3. Courbe de Température
        int tempY = pressureY + GRAPH_H + 14;
        g.drawString(font, "§7Temperature (°C)", PADDING, tempY, 0xAAAAAA, false);
        tempY += 10;
        renderTemperatureCurve(g, localEntries, graphX, tempY, graphW, TEMP_GRAPH_H, mouseX, mouseY, currentTick, minT, maxT);

        g.drawString(font, String.format("%.0f", maxT), PADDING, tempY, 0x666666, false);
        g.drawString(font, String.format("%.0f", (minT + maxT) / 2), PADDING, tempY + TEMP_GRAPH_H / 2 - 4, 0x666666, false);
        g.drawString(font, String.format("%.0f", minT), PADDING, tempY + TEMP_GRAPH_H - 8, 0x666666, false);

        if (minT <= 0f && maxT >= 0f) {
            int zeroTempY = tempY + TEMP_GRAPH_H - (int)((0f - minT) / (maxT - minT) * TEMP_GRAPH_H);
            g.fill(graphX, zeroTempY, graphX + graphW, zeroTempY + 1, 0x3311AAFF);
        }

        // 4. Courbe d'Humidité
        int humY = tempY + TEMP_GRAPH_H + 14;
        g.drawString(font, "§7Humidity (%)", PADDING, humY, 0xAAAAAA, false);
        humY += 10;
        renderHumidityCurve(g, localEntries, graphX, humY, graphW, HUM_GRAPH_H, mouseX, mouseY, currentTick);
        g.drawString(font, "100", PADDING, humY, 0x666666, false);
        g.drawString(font, "50",  PADDING, humY + HUM_GRAPH_H / 2 - 4, 0x666666, false);
        g.drawString(font, "0",   PADDING, humY + HUM_GRAPH_H - 8, 0x666666, false);

        // 5. Courbe du Vent
        int windGraphY = humY + HUM_GRAPH_H + 14;
        g.drawString(font, "§7Wind Speed (km/h)", PADDING, windGraphY, 0xAAAAAA, false);
        windGraphY += 10;
        renderWindCurve(g, localEntries, graphX, windGraphY, graphW, WIND_GRAPH_H, mouseX, mouseY, currentTick);

        g.drawString(font, String.format("%.0f", MAX_KMH),        PADDING, windGraphY, 0x666666, false);
        g.drawString(font, String.format("%.0f", MAX_KMH * 0.50f), PADDING, windGraphY + (WIND_GRAPH_H * 2 / 4) - 4, 0x666666, false);
        g.drawString(font, "0",                                 PADDING, windGraphY + WIND_GRAPH_H - 8, 0x666666, false);

        // 6. Informations de bas de page
        int infoY = windGraphY + WIND_GRAPH_H + 12;
        float pLocal   = ClientAtmosphereState.getLocalPressure();
        float trendVal = ClientAtmosphereState.getTrend() * 20 * 60;
        float f30      = ClientAtmosphereState.getForecast30();
        float windIntensity = ClientAtmosphereState.getWindIntensity();
        String cat = ClientAtmosphereState.getCategory();
        String trendSym = trendVal > 0.002f ? "§a↑" : trendVal < -0.002f ? "§c↓" : "§7→";

        g.drawString(font, "§7Current  §f" + String.format("%.1f", pLocal) + " hPa  §8[" + colorCategory(cat) + "§8]", PADDING, infoY, 0xFFFFFF, false);
        g.drawString(font, "§7Trend    " + trendSym + " §f" + String.format("%.2f", trendVal) + " hPa/min", PADDING, infoY + 10, 0xFFFFFF, false);
        g.drawString(font, "§7+30min   §f" + String.format("%.1f", f30) + " hPa", PADDING, infoY + 20, 0xFFFFFF, false);

        int col2X = width / 2 - 20;
        float curTemp = ClientAtmosphereState.getLocalTemperature();
        float curHum  = ClientAtmosphereState.getLocalHumidity();

        String tColor = curTemp <= -20f ? "§5" : curTemp <= 0f ? "§1" : curTemp < 15f ? "§b" : curTemp < 30f ? "§e" : curTemp < 55f ? "§c" : "§4";

        g.drawString(font, "§7Temperature  " + tColor + String.format("%.1f", curTemp) + " °C", col2X, infoY, 0xFFFFFF, false);
        g.drawString(font, "§7Humidity     §9" + String.format("%.0f%%", curHum), col2X, infoY + 10, 0xFFFFFF, false);
        g.drawString(font, "§7Wind Intens. §f" + String.format("%.3f", windIntensity), col2X, infoY + 20, 0xFFFFFF, false);

        float currentKmh = ClientWind.regionalKmh();
        g.drawString(font, "§7Wind Speed   §f" + String.format("%.1f", currentKmh) + " km/h  " + WindScale.beaufort(currentKmh).getString(), PADDING, infoY + 32, 0xFFFFFF, false);

        g.drawCenteredString(font, "§8[ESC] Close", width / 2, height - PADDING, 0x666666);

        if (hoveredEntry != null) {
            String timeText = hoveredTimeAgo == 0 ? "Now" : "-" + hoveredTimeAgo + " min ago";
            String fullText = "§b" + hoveredValueText + " §7(" + timeText + ")";

            int textW = font.width(fullText);
            int boxX = hoveredX + 8;
            int boxY = hoveredY - 14;

            if (boxX + textW + 6 > width) {
                boxX = hoveredX - textW - 10;
            }

            g.fill(boxX, boxY, boxX + textW + 6, boxY + 12, 0xEE000000);
            g.renderOutline(boxX, boxY, textW + 6, 12, 0x55FFFFFF);
            g.drawString(font, fullText, boxX + 3, boxY + 2, 0xFFFFFF, false);
        }
    }

    private void renderPressureCurve(GuiGraphics g, List<ClientLocalHistory.LocalEntry> entries, int x, int y, int w, int h, int mouseX, int mouseY, long currentTick, float minP, float maxP) {
        g.fill(x, y, x + w, y + h, 0x15FFFFFF);
        long minTick = entries.getFirst().gameTick();
        long maxTick = entries.getLast().gameTick();
        long range   = maxTick - minTick == 0 ? 1 : maxTick - minTick;

        for (int i = 0; i < entries.size(); i++) {
            ClientLocalHistory.LocalEntry a = entries.get(i);
            int x1 = x + (int)((a.gameTick() - minTick) * w / range);
            int y1 = y + h - (int)(Mth.clamp((a.localPressure() - minP) / (maxP - minP), 0f, 1f) * h);

            g.fill(x1 - 1, y1 - 1, x1 + 1, y1 + 1, 0xFFAADDFF);

            if (i < entries.size() - 1) {
                ClientLocalHistory.LocalEntry b = entries.get(i + 1);
                int x2 = x + (int)((b.gameTick() - minTick) * w / range);
                int y2 = y + h - (int)(Mth.clamp((b.localPressure() - minP) / (maxP - minP), 0f, 1f) * h);
                drawLine(g, x1, y1, x2, y2, 0xFF66AAFF);
            }

            if (mouseX >= x1 - 2 && mouseX <= x1 + 2 && mouseY >= y && mouseY <= y + h) {
                g.fill(mouseX, y, mouseX + 1, y + h, 0x33FFFFFF);
                hoveredEntry = a;
                hoveredX = mouseX;
                hoveredY = mouseY;
                hoveredValueText = String.format("%.1f hPa", a.localPressure());
                hoveredTimeAgo = (currentTick - a.gameTick()) / 1200;
            }
        }
    }

    private void renderTemperatureCurve(GuiGraphics g, List<ClientLocalHistory.LocalEntry> entries, int x, int y, int w, int h, int mouseX, int mouseY, long currentTick, float minT, float maxT) {
        g.fill(x, y, x + w, y + h, 0x15FFFFFF);
        long minTick = entries.getFirst().gameTick();
        long maxTick = entries.getLast().gameTick();
        long range   = maxTick - minTick == 0 ? 1 : maxTick - minTick;

        for (int i = 0; i < entries.size(); i++) {
            ClientLocalHistory.LocalEntry a = entries.get(i);
            int x1 = x + (int)((a.gameTick() - minTick) * w / range);

            float tempA = a.localTemperature();
            int y1 = y + h - (int)((tempA - minT) / (maxT - minT) * h);

            g.fill(x1 - 1, y1 - 1, x1 + 1, y1 + 1, getTemperatureColor(tempA, 0xFF));

            if (i < entries.size() - 1) {
                ClientLocalHistory.LocalEntry b = entries.get(i + 1);
                int x2 = x + (int)((b.gameTick() - minTick) * w / range);

                float tempB = b.localTemperature();
                int y2 = y + h - (int)((tempB - minT) / (maxT - minT) * h);
                drawLine(g, x1, y1, x2, y2, getTemperatureColor((tempA + tempB) / 2f, 0xBB));
            }

            if (mouseX >= x1 - 2 && mouseX <= x1 + 2 && mouseY >= y && mouseY <= y + h) {
                g.fill(mouseX, y, mouseX + 1, y + h, 0x33FFFFFF);
                hoveredEntry = a;
                hoveredX = mouseX;
                hoveredY = mouseY;
                hoveredValueText = String.format("%.1f °C", a.localTemperature());
                hoveredTimeAgo = (currentTick - a.gameTick()) / 1200;
            }
        }
    }

    private void renderHumidityCurve(GuiGraphics g, List<ClientLocalHistory.LocalEntry> entries, int x, int y, int w, int h, int mouseX, int mouseY, long currentTick) {
        g.fill(x, y, x + w, y + h, 0x15FFFFFF);
        long minTick = entries.getFirst().gameTick();
        long maxTick = entries.getLast().gameTick();
        long range   = maxTick - minTick == 0 ? 1 : maxTick - minTick;
        float minH = 0f, maxH = 100f;

        for (int i = 0; i < entries.size(); i++) {
            ClientLocalHistory.LocalEntry a = entries.get(i);
            int x1 = x + (int)((a.gameTick() - minTick) * w / range);
            float humA = Mth.clamp(a.localHumidity(), minH, maxH);
            int y1 = y + h - (int)((humA - minH) / (maxH - minH) * h);

            g.fill(x1 - 1, y1 - 1, x1 + 1, y1 + 1, 0xFF88AAFF);

            if (i < entries.size() - 1) {
                ClientLocalHistory.LocalEntry b = entries.get(i + 1);
                int x2 = x + (int)((b.gameTick() - minTick) * w / range);
                float humB = Mth.clamp(b.localHumidity(), minH, maxH);
                int y2 = y + h - (int)((humB - minH) / (maxH - minH) * h);
                drawLine(g, x1, y1, x2, y2, 0xBB3366FF);
            }

            if (mouseX >= x1 - 2 && mouseX <= x1 + 2 && mouseY >= y && mouseY <= y + h) {
                g.fill(mouseX, y, mouseX + 1, y + h, 0x33FFFFFF);
                hoveredEntry = a;
                hoveredX = mouseX;
                hoveredY = mouseY;
                hoveredValueText = String.format("%.0f %%", humA);
                hoveredTimeAgo = (currentTick - a.gameTick()) / 1200;
            }
        }
    }

    private void renderWindCurve(GuiGraphics g, List<ClientLocalHistory.LocalEntry> entries, int x, int y, int w, int h, int mouseX, int mouseY, long currentTick) {
        g.fill(x, y, x + w, y + h, 0x15FFFFFF);
        long minTick = entries.getFirst().gameTick();
        long maxTick = entries.getLast().gameTick();
        long range   = maxTick - minTick == 0 ? 1 : maxTick - minTick;

        for (int i = 0; i < entries.size(); i++) {
            ClientLocalHistory.LocalEntry a = entries.get(i);
            int x1 = x + (int)((a.gameTick() - minTick) * w / range);
            float kmhA = WindScale.intensityToKmh(a.localWind());
            int y1 = y + h - (int)(Mth.clamp(kmhA / MAX_KMH, 0f, 1f) * h);

            g.fill(x1 - 1, y1 - 1, x1 + 1, y1 + 1, 0xFFAAFFFA);

            if (i < entries.size() - 1) {
                ClientLocalHistory.LocalEntry b = entries.get(i + 1);
                int x2 = x + (int)((b.gameTick() - minTick) * w / range);
                float kmhB = WindScale.intensityToKmh(b.localWind());
                int y2 = y + h - (int)(Mth.clamp(kmhB / MAX_KMH, 0f, 1f) * h);
                drawLine(g, x1, y1, x2, y2, 0xFF55FFFF);
            }

            if (mouseX >= x1 - 2 && mouseX <= x1 + 2 && mouseY >= y && mouseY <= y + h) {
                g.fill(mouseX, y, mouseX + 1, y + h, 0x33FFFFFF);
                hoveredEntry = a;
                hoveredX = mouseX;
                hoveredY = mouseY;
                hoveredValueText = String.format("%.1f km/h (%s)", kmhA, WindScale.beaufort(kmhA).getString());
                hoveredTimeAgo = (currentTick - a.gameTick()) / 1200;
            }
        }
    }

    private void renderWeatherBars(GuiGraphics g, List<ClientLocalHistory.LocalEntry> entries, int x, int y, int w, int h, int mouseX, int mouseY, long currentTick) {
        g.fill(x, y, x + w, y + h, 0x22FFFFFF);
        long minTick = entries.getFirst().gameTick();
        long maxTick = entries.getLast().gameTick();
        long range   = maxTick - minTick == 0 ? 1 : maxTick - minTick;

        for (int i = 0; i < entries.size(); i++) {
            ClientLocalHistory.LocalEntry curr = entries.get(i);

            int x1 = x + (int)((curr.gameTick() - minTick) * w / range);
            int x2 = x + w;

            if (i < entries.size() - 1) {
                x2 = x + (int)((entries.get(i + 1).gameTick() - minTick) * w / range);
            }

            if (x2 > x1 && curr.weather() != null) {
                g.fill(x1, y, x2, y + h, getWeatherColor(curr.weather()));
            }

            if (mouseX >= x1 && mouseX <= x2 && mouseY >= y && mouseY <= y + h) {
                g.fill(mouseX, y, mouseX + 1, y + h, 0x33FFFFFF);
                hoveredEntry = curr;
                hoveredX = mouseX;
                hoveredY = mouseY;
                hoveredValueText = curr.weather() != null ? curr.weather().name() : "Unknown";
                hoveredTimeAgo = (currentTick - curr.gameTick()) / 1200;
            }
        }
    }

    private void drawLine(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int steps = Math.max(dx, dy);
        if (steps == 0) {
            g.fill(x1, y1, x1 + 1, y1 + 1, color);
            return;
        }
        for (int i = 0; i <= steps; i++) {
            int px = x1 + (x2 - x1) * i / steps;
            int py = y1 + (y2 - y1) * i / steps;
            g.fill(px, py, px + 1, py + 1, color);
        }
    }

    private static int getWeatherColor(WeatherTypes type) {
        return switch (type) {
            case CLEAR           -> 0xAA87CEEB;
            case SUNNY           -> 0xAA79B9D3;
            case CLOUDY          -> 0xAAB0B8C0;
            case MIST            -> 0xAAC8CDD0;
            case OVERCAST        -> 0xAA708090;
            case DRIZZLE         -> 0xAA6A8FAA;
            case LIGHT_RAIN      -> 0xAA4A78AA;
            case HEAVY_RAIN      -> 0xAA2A5888;
            case FREEZING_RAIN   -> 0xAA88AACC;
            case THUNDERSTORM    -> 0xAA3A3F5A;
            case SNOW            -> 0xAADDEEFF;
            case BLIZZARD        -> 0xAAAABBCC;
            case HAIL            -> 0xAABBBB99;
            case FOG             -> 0xAAA0A0A8;
            case DENSE_FOG       -> 0xAA707078;
            case WINDY           -> 0xAA90C890;
            case SANDSTORM       -> 0xAAD9B05C;
            case BRIMSTONE_STORM -> 0xAACC4400;
            case LAVA_RAIN       -> 0xAAFF5500;
            case ASH_STORM       -> 0xAA666655;
            case NETHERSTORM     -> 0xAAB01000;
            case HELLFIRE        -> 0xAAFF3300;
            case VOID_STORM      -> 0xAA221133;
            case END_MIST        -> 0xAA8866AA;
            case CHORUS_GALE     -> 0xAA664488;
            case ENDERSTORM      -> 0xAA440066;
            default -> 0xAA888888;
        };
    }

    private static String colorCategory(String cat) {
        return switch (cat) {
            case "HIGH"    -> "§aHIGH";
            case "NEUTRAL" -> "§eNEUTRAL";
            case "LOW"     -> "§cLOW";
            case "STORM"   -> "§4STORM";
            default        -> "§7" + cat;
        };
    }

    private static int getTemperatureColor(float temp, int alpha) {
        int r, g, b;

        if (temp <= -20f) {
            float factor = Mth.clamp((temp - (-60f)) / 40f, 0f, 1f);
            r = (int) Mth.lerp(factor, 25, 40);
            g = (int) Mth.lerp(factor, 5, 100);
            b = (int) Mth.lerp(factor, 50, 220);
        } else if (temp <= 0f) {
            float factor = Mth.clamp((temp - (-20f)) / 20f, 0f, 1f);
            r = (int) Mth.lerp(factor, 40, 180);
            g = (int) Mth.lerp(factor, 100, 230);
            b = (int) Mth.lerp(factor, 220, 255);
        } else if (temp <= 18f) {
            float factor = Mth.clamp((temp - 0f) / 18f, 0f, 1f);
            r = (int) Mth.lerp(factor, 180, 235);
            g = (int) Mth.lerp(factor, 230, 215);
            b = (int) Mth.lerp(factor, 255, 150);
        } else if (temp <= 35f) {
            float factor = Mth.clamp((temp - 18f) / 17f, 0f, 1f);
            r = (int) Mth.lerp(factor, 235, 245);
            g = (int) Mth.lerp(factor, 215, 120);
            b = (int) Mth.lerp(factor, 150, 30);
        } else if (temp <= 65f) {
            float factor = Mth.clamp((temp - 35f) / 30f, 0f, 1f);
            r = (int) Mth.lerp(factor, 245, 220);
            g = (int) Mth.lerp(factor, 120, 0);
            b = (int) Mth.lerp(factor, 30, 0);
        } else {
            float factor = Mth.clamp((temp - 65f) / 55f, 0f, 1f);
            r = (int) Mth.lerp(factor, 220, 65);
            g = (int) Mth.lerp(factor, 0, 0);
            b = (int) Mth.lerp(factor, 0, 5);
        }

        return (alpha << 24) | (r << 16) | (g << 8) | b;
    }

    @Override
    public boolean isPauseScreen() { return false; }
}