package net.antopfr.advancedweather.content.block.archive;

import net.antopfr.advancedweather.AdvancedWeather;
import net.antopfr.advancedweather.config.AWCommonConfig;
import net.antopfr.advancedweather.content.WeatherRecord;
import net.antopfr.advancedweather.util.Key;
import net.antopfr.advancedweather.util.ValueColors;
import net.antopfr.advancedweather.weather.WeatherTypes;
import net.antopfr.advancedweather.weather.wind.WindScale;
import net.antopfr.advancedweather.compat.catnip.CatnipCompat;
import net.antopfr.advancedweather.compat.catnip.Smoothed;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class WeatherArchiveScreen extends Screen {

    private static final int PANEL_W = 300;
    private static final int PANEL_H = 200;
    private static final int GRAPH_MARGIN_L = 38;
    private static final int GRAPH_MARGIN_R = 12;
    private static final int GRAPH_MARGIN_T = 68; //was 58
    private static final int GRAPH_MARGIN_B = 26;

    private static final long MIN_WINDOW = 6000L;
    private long viewWindowTicks = 24000L;
    private long viewEndTime = -1;

    private enum Series {
        TEMPERATURE("advancedweather.temp", "°C") {
            @Override float value(WeatherRecord r) { return r.temperature(); }
            @Override int color(float v) { return ValueColors.temperature(v); }
            @Override boolean present(WeatherRecord r) { return r.hasTemperature(); }
        },
        PRESSURE("advancedweather.pressure", "hPa") {
            @Override float value(WeatherRecord r) { return r.pressure(); }
            @Override int color(float v) { return 0xE0E0E0; }
            @Override boolean present(WeatherRecord r) { return r.hasPressure(); }
        },
        HUMIDITY("advancedweather.humidity", "%") {
            @Override float value(WeatherRecord r) { return r.humidity(); }
            @Override int color(float v) { return ValueColors.humidity(v); }
            @Override boolean present(WeatherRecord r) { return r.hasHumidity(); }
        },
        WIND("advancedweather.wind", "km/h") {
            @Override float value(WeatherRecord r) {
                return WindScale.intensityToKmh(r.windIntensity());
            }
            @Override int color(float v) { return ValueColors.wind(v); }
            @Override boolean present(WeatherRecord r) { return r.hasWind(); }
        };

        final String labelKey, unit;
        Series(String labelKey, String unit) { this.labelKey = labelKey; this.unit = unit; }
        abstract float value(WeatherRecord r);
        abstract int color(float v);
        abstract boolean present(WeatherRecord r);
    }

    private record PlotPoint(long time, float avg, float min, float max,
                             WeatherRecord sample, int count) {}

    private final WeatherArchiveBlockEntity archive;
    private Series current = Series.TEMPERATURE;

    private float openProgress = 0f;
    private final Smoothed viewMin = CatnipCompat.smoothed();
    private final Smoothed viewMax = CatnipCompat.smoothed();
    private boolean rangeInitialized = false;

    private int hoveredPoint = -1;
    private WeatherRecord forecastPoint;
    private List<PlotPoint> plotted = List.of();

    private int lastRecordCount = -1;

    public WeatherArchiveScreen(WeatherArchiveBlockEntity archive) {
        super(Component.translatable("block.advancedweather.weather_archive"));
        this.archive = archive;
    }

    @Override
    protected void init() {
        super.init();
        openProgress = 0f;
        rangeInitialized = false;
        forecastPoint = archive.computeForecastPoint();
    }

    private long earliestTime() {
        var recs = archive.getRecords();
        return recs.isEmpty() ? 0 : recs.getFirst().gameTime();
    }

    private long latestTime() {
        var recs = archive.getRecords();
        long last = recs.isEmpty() ? 0 : recs.getLast().gameTime();
        return forecastPoint != null ? Math.max(last, forecastPoint.gameTime()) : last;
    }

    private long windowEnd() {
        return viewEndTime == -1 ? latestTime() : viewEndTime;
    }

    private long windowStart() {
        return windowEnd() - viewWindowTicks;
    }

    private boolean followingPresent() {
        return viewEndTime == -1;
    }

    @Override
    public void tick() {
        super.tick();

        List<WeatherRecord> records = archive.getRecords();

        if (records.size() != lastRecordCount) {
            lastRecordCount = records.size();
            forecastPoint = archive.computeForecastPoint();
        }

        if (records.isEmpty()) return;

        long tMin = windowStart();
        long tMax = windowEnd();

        float min = Float.MAX_VALUE, max = -Float.MAX_VALUE;
        boolean any = false;
        for (WeatherRecord r : records) {
            if (!current.present(r)) continue;
            if (r.gameTime() < tMin || r.gameTime() > tMax) continue;
            float v = current.value(r);
            min = Math.min(min, v);
            max = Math.max(max, v);
            any = true;
        }
        if (forecastPoint != null && current.present(forecastPoint) && followingPresent()
                && forecastPoint.gameTime() >= tMin && forecastPoint.gameTime() <= tMax) {
            float v = current.value(forecastPoint);
            min = Math.min(min, v);
            max = Math.max(max, v);
            any = true;
        }
        if (!any) return;

        float pad = Math.max((max - min) * 0.1f, 0.5f);
        min -= pad; max += pad;

        if (!rangeInitialized) {
            viewMin.startWithValue(min);
            viewMax.startWithValue(max);
            rangeInitialized = true;
        }
        viewMin.chase(min, 0.3);
        viewMax.chase(max, 0.3);
        viewMin.tickChaser();
        viewMax.tickChaser();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        openProgress = Math.min(1f, openProgress + partialTick * 0.12f);
        float ease = 1f - (1f - openProgress) * (1f - openProgress);

        g.fill(0, 0, width, height, ((int) (ease * 0xB0)) << 24);
        super.render(g, mouseX, mouseY, partialTick);

        int x = (width - PANEL_W) / 2;
        int y = (int) Mth.lerp(ease, (height - PANEL_H) / 2f + 40, (height - PANEL_H) / 2f);
        int alpha = Math.max(4, (int) (ease * 0xFF));

        g.fill(x, y, x + PANEL_W, y + PANEL_H, ((int) (ease * 0xE8) << 24) | 0x0C1014);
        g.fill(x, y, x + PANEL_W, y + 18, (alpha << 24) | 0x1E2630);
        g.drawCenteredString(font, "§l" + Key.t("block.advancedweather.weather_archive"), x + PANEL_W / 2, y + 5,
                withAlpha(0xFFFFFF, alpha));

        List<WeatherRecord> records = archive.getRecords();

        drawTabs(g, x, y + 22, mouseX, mouseY, alpha);

        List<int[]> top = archive.getPredictedTop();
        int forecastLineY = y + 44;
        if (!top.isEmpty()) {
            long age = archive.dataAge();
            long aging = AWCommonConfig.get().archiveAgingDataTicks;
            long outdated = AWCommonConfig.get().archiveOutdatedDataTicks;

            if (age > outdated) {
                WeatherTypes first = WeatherTypes.values()[top.getFirst()[0]];
                drawWeatherIcon(g, first, x + 8, forecastLineY - 2, 12);
                g.drawString(font, "§8" + Key.label("advancedweather.archive.next") + "§m" + first.displayString()
                                + "§r §c[" + Key.t("advancedweather.archive.outdated") + "]",
                        x + 24, forecastLineY, withAlpha(0x888888, alpha), true);
            } else {
                int tx = x + 8;
                String next = Key.label("advancedweather.archive.next");
                g.drawString(font, "§7" + next, tx, forecastLineY, withAlpha(0xAAAAAA, alpha), true);
                tx += font.width(next) + 2;

                for (int i = 0; i < top.size(); i++) {
                    WeatherTypes type = WeatherTypes.values()[top.get(i)[0]];
                    float pct = top.get(i)[1] / 10f;
                    if (i > 0 && pct < 10f) break;

                    drawWeatherIcon(g, type, tx, forecastLineY - 2, 12);
                    tx += 14;
                    String text = i == 0
                            ? String.format("%s §8%.0f%%", type.displayString(), pct)
                            : String.format("§8%.0f%%", pct);
                    g.drawString(font, text, tx, forecastLineY,
                            withAlpha(i == 0 ? 0xFFFFFF : 0x999999, alpha), true);
                    tx += font.width(ChatFormatting.stripFormatting(text)) + 8;
                }
                if (age > aging) {
                    g.drawString(font, "§6[" + Key.t("advancedweather.archive.aging") + "]", tx, forecastLineY, withAlpha(0xCC8833, alpha), true);
                }

                float conf = archive.getConfidence();
                int barX = x + 8;
                int barY = forecastLineY + 11;
                int segW = 6, segGap = 2, segs = 5;
                int filled = Math.round(conf * segs);
                for (int i = 0; i < segs; i++) {
                    int sx = barX + i * (segW + segGap);
                    int segColor = i < filled ? confidenceColor(conf) : 0x333B42;
                    g.fill(sx, barY, sx + segW, barY + 4, (alpha << 24) | segColor);
                }
                int labelX = barX + segs * (segW + segGap) + 4;
                g.drawString(font, confidenceLabel(conf),
                        labelX, barY - 2, withAlpha(0xFFFFFF, alpha), true);
            }
        } else {
            int needed = AWCommonConfig.get().archiveMinRecordsForForecast;
            int remaining = needed - Math.min(needed, records.size());
            g.drawString(font, "§8" + Component.translatable("advancedweather.archive.needs_records", remaining).getString(),
                    x + 8, forecastLineY, withAlpha(0x555555, alpha), true);
        }

        drawGraph(g, x, y, records, mouseX, mouseY, alpha, partialTick);

        String count = Component.translatable(records.size() > 1 ? "advancedweather.archive.records"
                : "advancedweather.archive.record", records.size()).getString();
        g.drawString(font, "§8" + count, x + 8, y + PANEL_H - 12, withAlpha(0x555555, alpha), false);

        if (!followingPresent()) {
            String hist = "⏸ history - press → to return";
            g.drawString(font, "§6" + hist, x + PANEL_W - 10 - font.width(hist),
                    y + PANEL_H - 12, withAlpha(0xCC8833, alpha), false);
        } else {
            String zoom = windowLabel();
            g.drawString(font, "§8" + zoom, x + PANEL_W - 10 - font.width(zoom),
                    y + PANEL_H - 12, withAlpha(0x555555, alpha), false);
        }
    }

    private String windowLabel() {
        long w = viewWindowTicks;
        if (w >= 24000L) {
            float days = w / 24000f;
            return days == Math.floor(days) ? (int) days + "d window" : String.format("%.1fd window", days);
        }
        return (w / 1000L) + "h window";
    }

    private void drawTabs(GuiGraphics g, int panelX, int tabY, int mouseX, int mouseY, int alpha) {
        int tabW = 62, tabH = 16;
        int tx = panelX + 8;
        for (Series s : Series.values()) {
            boolean active = s == current;
            boolean hovered = mouseX >= tx && mouseX < tx + tabW && mouseY >= tabY && mouseY < tabY + tabH;
            int bg = active ? 0x2A4A66 : hovered ? 0x30404C : 0x1A2228;
            g.fill(tx, tabY, tx + tabW, tabY + tabH, (alpha << 24) | bg);
            g.drawCenteredString(font, Key.t(s.labelKey), tx + tabW / 2, tabY + 4,
                    withAlpha(active ? 0x7FD4FF : 0xAAAAAA, alpha));
            tx += tabW + 4;
        }
    }

    private List<PlotPoint> downsample(List<WeatherRecord> visible, long tMin, long tMax, int gw) {
        int maxPoints = Math.max(4, gw / 2);
        if (visible.size() <= maxPoints) {
            List<PlotPoint> out = new ArrayList<>(visible.size());
            for (WeatherRecord r : visible) {
                float v = current.value(r);
                out.add(new PlotPoint(r.gameTime(), v, v, v, r, 1));
            }
            return out;
        }

        long bucketSpan = Math.max(1, (tMax - tMin) / maxPoints);
        Map<Long, List<WeatherRecord>> buckets = new LinkedHashMap<>();
        for (WeatherRecord r : visible) {
            buckets.computeIfAbsent((r.gameTime() - tMin) / bucketSpan, k -> new ArrayList<>()).add(r);
        }

        List<PlotPoint> out = new ArrayList<>(buckets.size());
        for (List<WeatherRecord> bucket : buckets.values()) {
            float sum = 0, min = Float.MAX_VALUE, max = -Float.MAX_VALUE;
            long tSum = 0;
            for (WeatherRecord r : bucket) {
                float v = current.value(r);
                sum += v;
                min = Math.min(min, v);
                max = Math.max(max, v);
                tSum += r.gameTime();
            }
            out.add(new PlotPoint(tSum / bucket.size(), sum / bucket.size(), min, max,
                    bucket.getLast(), bucket.size()));
        }
        return out;
    }

    private void drawGraph(GuiGraphics g, int panelX, int panelY, List<WeatherRecord> records,
                           int mouseX, int mouseY, int alpha, float partialTick) {
        int gx = panelX + GRAPH_MARGIN_L;
        int gy = panelY + GRAPH_MARGIN_T;
        int gw = PANEL_W - GRAPH_MARGIN_L - GRAPH_MARGIN_R;
        int gh = PANEL_H - GRAPH_MARGIN_T - GRAPH_MARGIN_B;

        long tMin = windowStart();
        long tMax = windowEnd();
        if (tMax <= tMin) tMax = tMin + 1;

        final long fMin = tMin, fMax = tMax;
        List<WeatherRecord> visible = records.stream()
                .filter(current::present)
                .filter(r -> r.gameTime() >= fMin && r.gameTime() <= fMax)
                .toList();
        boolean forecastVisible = forecastPoint != null && current.present(forecastPoint)
                && followingPresent()
                && forecastPoint.gameTime() >= tMin && forecastPoint.gameTime() <= tMax;

        g.fill(gx, gy, gx + gw, gy + gh, (Math.min(alpha, 0x60) << 24));

        if (visible.isEmpty()) {
            String msg = records.stream().anyMatch(current::present)
                    ? "§8" + Key.t("advancedweather.archive.no_data_window")
                    : "§8" + Key.t("advancedweather.archive.no_data_series");
            g.drawCenteredString(font, msg, gx + gw / 2, gy + gh / 2 - 4, withAlpha(0x555555, alpha));
            hoveredPoint = -1;
            plotted = List.of();
            drawTimeAxis(g, gx, gy, gw, gh, tMin, tMax, alpha);
            return;
        }

        float vMin = viewMin.getValue(partialTick);
        float vMax = viewMax.getValue(partialTick);
        if (vMax - vMin < 0.001f) vMax = vMin + 1f;

        for (int i = 0; i <= 4; i++) {
            int lineY = gy + gh - (i * gh / 4);
            g.fill(gx, lineY, gx + gw, lineY + 1, (Math.min(alpha, 0x28) << 24) | 0xFFFFFF);
            float labelVal = vMin + (vMax - vMin) * i / 4f;
            String lbl = String.format("%.0f", labelVal);
            g.drawString(font, lbl, gx - font.width(lbl) - 3, lineY - 3,
                    withAlpha(0x666666, alpha), false);
        }

        drawTimeAxis(g, gx, gy, gw, gh, tMin, tMax, alpha);

        plotted = downsample(visible, tMin, tMax, gw);
        hoveredPoint = -1;

        long span = tMax - tMin;

        g.enableScissor(gx, gy, gx + gw, gy + gh);

        for (PlotPoint p : plotted) {
            if (p.count() <= 1 || p.max() - p.min() < 0.001f) continue;
            int px = gx + (int) ((p.time() - tMin) * (long) gw / span);
            int pyMin = gy + gh - (int) ((p.min() - vMin) / (vMax - vMin) * gh);
            int pyMax = gy + gh - (int) ((p.max() - vMin) / (vMax - vMin) * gh);
            g.fill(px, pyMax, px + 1, pyMin + 1, (Math.min(alpha, 0x38) << 24) | 0xFFFFFF);
        }

        int prevX = -1, prevY = -1;
        for (PlotPoint p : plotted) {
            int px = gx + (int) ((p.time() - tMin) * (long) gw / span);
            int py = gy + gh - (int) ((p.avg() - vMin) / (vMax - vMin) * gh);
            if (prevX >= 0) {
                drawLine(g, prevX, prevY, px, py, withAlpha(0xFFFFFF, Math.min(alpha, 0x50)));
            }
            prevX = px; prevY = py;
        }

        for (int i = 0; i < plotted.size(); i++) {
            PlotPoint p = plotted.get(i);
            int px = gx + (int) ((p.time() - tMin) * (long) gw / span);
            int py = gy + gh - (int) ((p.avg() - vMin) / (vMax - vMin) * gh);

            boolean hovered = Math.abs(mouseX - px) <= 4 && Math.abs(mouseY - py) <= 4;
            if (hovered) hoveredPoint = i;

            int size = hovered ? 3 : 2;
            int color = current.color(p.avg());
            g.fill(px - size, py - size, px + size + 1, py + size + 1, (alpha << 24) | color);
        }

        g.disableScissor();

        if (forecastVisible && prevX >= 0) {
            int fx = gx + (int) ((forecastPoint.gameTime() - tMin) * (long) gw / span);
            int fy = gy + gh - (int) ((current.value(forecastPoint) - vMin) / (vMax - vMin) * gh);
            drawDottedLine(g, prevX, prevY, fx, fy, withAlpha(0x7FD4FF, Math.min(alpha, 0x90)));
        }

        if (forecastVisible) {
            int fx = gx + (int) ((forecastPoint.gameTime() - tMin) * (long) gw / span);
            int fy = gy + gh - (int) ((current.value(forecastPoint) - vMin) / (vMax - vMin) * gh);
            boolean hovered = Math.abs(mouseX - fx) <= 4 && Math.abs(mouseY - fy) <= 4;
            if (hovered) hoveredPoint = -2;

            int s = hovered ? 4 : 3;
            int c = (alpha << 24) | 0x7FD4FF;
            g.fill(fx - s, fy - s, fx + s + 1, fy - s + 1, c);
            g.fill(fx - s, fy + s, fx + s + 1, fy + s + 1, c);
            g.fill(fx - s, fy - s, fx - s + 1, fy + s + 1, c);
            g.fill(fx + s, fy - s, fx + s + 1, fy + s + 1, c);
        }

        if (hoveredPoint == -2) {
            drawPointTooltip(g, mouseX, mouseY, forecastPoint, true, null);
        } else if (hoveredPoint >= 0) {
            PlotPoint p = plotted.get(hoveredPoint);
            drawPointTooltip(g, mouseX, mouseY, p.sample(), false, p.count() > 1 ? p : null);
        }
    }

    private void drawTimeAxis(GuiGraphics g, int gx, int gy, int gw, int gh,
                              long tMin, long tMax, int alpha) {
        g.drawString(font, "§8" + timeLabel(tMin), gx, gy + gh + 4, withAlpha(0x666666, alpha), false);
        String endLbl = timeLabel(tMax);
        g.drawString(font, "§8" + endLbl, gx + gw - font.width(endLbl), gy + gh + 4,
                withAlpha(0x666666, alpha), false);
    }

    private static String timeLabel(long gameTime) {
        long day = gameTime / 24000L;
        int hours = (int) ((gameTime + 6000) / 1000 % 24);
        return Key.t("advancedweather.archive.day_short") + day + " " + String.format("%02dh", hours);
    }

    private void drawPointTooltip(GuiGraphics g, int mouseX, int mouseY, WeatherRecord r,
                                  boolean forecast, PlotPoint aggregated) {
        long day = r.gameTime() / 24000L;
        int hours = (int) ((r.gameTime() + 6000) / 1000 % 24);
        int minutes = (int) ((r.gameTime() % 1000) * 60 / 1000);

        List<Component> lines = new ArrayList<>();
        if (forecast) lines.add(Component.literal("§b§l" + Key.t("advancedweather.archive.forecast_point")));
        lines.add(Component.translatable("advancedweather.time.day_clock", day,
                String.format("%02d:%02d", hours, minutes)).withStyle(ChatFormatting.GRAY));
        if (!forecast) lines.add(r.weatherType().displayName().copy().withStyle(ChatFormatting.WHITE));

        if (aggregated != null) {
            lines.add(Component.literal(String.format("§7%s§f%s %.1f %s", Key.label(current.labelKey),
                    Key.t("advancedweather.archive.average"), aggregated.avg(), current.unit)));
            lines.add(Component.literal(String.format("§8%.1f – %.1f (%s)", aggregated.min(), aggregated.max(),
                    Component.translatable("advancedweather.archive.samples", aggregated.count()).getString())));
        } else {
            lines.add(Component.literal(String.format("§7%s§f%.1f %s",
                    Key.label(current.labelKey), current.value(r), current.unit)));
        }

        if (!forecast) {
            if (r.stationName().isBlank()) {
                lines.add(Component.literal(String.format("§8%s", r.biome().getPath())));
                lines.add(Component.literal(String.format("§8%d, %d, %d",
                        r.pos().getX(), r.pos().getY(), r.pos().getZ())));
            } else {
                lines.add(Component.literal("§b" + r.stationName()));
            }
        }

        g.renderComponentTooltip(font, lines, mouseX, mouseY);
    }

    private void drawLine(GuiGraphics g, int x0, int y0, int x1, int y1, int color) {
        int dx = Math.abs(x1 - x0), dy = -Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1, sy = y0 < y1 ? 1 : -1;
        int err = dx + dy;
        while (true) {
            g.fill(x0, y0, x0 + 1, y0 + 1, color);
            if (x0 == x1 && y0 == y1) break;
            int e2 = 2 * err;
            if (e2 >= dy) { err += dy; x0 += sx; }
            if (e2 <= dx) { err += dx; y0 += sy; }
        }
    }

    private void drawDottedLine(GuiGraphics g, int x0, int y0, int x1, int y1, int color) {
        int steps = Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0));
        if (steps == 0) return;
        for (int i = 0; i <= steps; i++) {
            if ((i / 3) % 2 == 0) {
                int px = x0 + (x1 - x0) * i / steps;
                int py = y0 + (y1 - y0) * i / steps;
                g.fill(px, py, px + 1, py + 1, color);
            }
        }
    }

    private void drawWeatherIcon(GuiGraphics g, WeatherTypes type, int x, int y, int size) {
        ResourceLocation icon = ResourceLocation.fromNamespaceAndPath(AdvancedWeather.MOD_ID,
                "textures/gui/weather_icons/" + type.name().toLowerCase() + ".png");
        g.blit(icon, x, y, size, size, 0f, 0f, 32, 32, 32, 32);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        long step = viewWindowTicks / 4;
        if (keyCode == 263) {
            if (viewEndTime == -1) viewEndTime = latestTime();
            long minEnd = earliestTime() + Math.min(viewWindowTicks, latestTime() - earliestTime());
            viewEndTime = Math.max(minEnd, viewEndTime - step);
            rangeInitialized = false;
            return true;
        }
        if (keyCode == 262) {
            if (viewEndTime != -1) {
                viewEndTime += step;
                if (viewEndTime >= latestTime()) viewEndTime = -1;
            }
            rangeInitialized = false;
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        long total = Math.max(MIN_WINDOW, latestTime() - earliestTime() + 2000L);
        long old = viewWindowTicks;
        if (scrollY > 0) {
            viewWindowTicks = Math.max(MIN_WINDOW, (long) (viewWindowTicks / 1.5));
            rangeInitialized = false;
        } else {
            viewWindowTicks = Math.min(total, (long) (viewWindowTicks * 1.5));
            rangeInitialized = false;
        }
        return viewWindowTicks != old || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int x = (width - PANEL_W) / 2;
            int y = (height - PANEL_H) / 2;
            int tabW = 62, tabH = 16, tabY = y + 22;
            int tx = x + 8;
            for (Series s : Series.values()) {
                if (mouseX >= tx && mouseX < tx + tabW && mouseY >= tabY && mouseY < tabY + tabH) {
                    if (s != current) {
                        current = s;
                        rangeInitialized = false;
                    }
                    return true;
                }
                tx += tabW + 4;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private static int withAlpha(int rgb, int alpha) {
        return (Mth.clamp(alpha, 0, 255) << 24) | (rgb & 0x00FFFFFF);
    }

    private static String confidenceLabel(float c) {
        if (c < 0.30f) return Key.c("§8", "advancedweather.uncertain");
        if (c < 0.50f) return Key.c("§7", "advancedweather.possible");
        if (c < 0.75f) return Key.c("§a", "advancedweather.likely");
        return Key.c("§2", "advancedweather.very_likely");
    }

    private static int confidenceColor(float c) {
        if (c < 0.30f) return 0x777777;
        if (c < 0.50f) return 0xAAAA55;
        if (c < 0.75f) return 0x55AA55;
        return 0x33CC44;
    }
}