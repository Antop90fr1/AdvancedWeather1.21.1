package net.antopfr.advancedweather.content.block.autosampler;

import net.antopfr.advancedweather.network.toserver.SetSamplerIntervalPacket;
import net.antopfr.advancedweather.util.Key;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class AutoSamplerScreen extends Screen {

    private static final int PANEL_W = 210;
    private static final int PANEL_H = 140;

    private record Preset(String label, int ticks) {}
    private static final Preset[] PRESETS = {
            new Preset("5s", 100), new Preset("30s", 600), new Preset("1m", 1200),
            new Preset("5m", 6000), new Preset("15m", 18000), new Preset("1h", 72000)
    };

    private final AutoSamplerBlockEntity sampler;
    private float openProgress = 0f;
    private int hoveredPreset = -1;

    public AutoSamplerScreen(AutoSamplerBlockEntity sampler) {
        super(Component.translatable("block.advancedweather.auto_sampler"));
        this.sampler = sampler;
    }

    @Override
    public void render(@NotNull GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        openProgress = Math.min(1f, openProgress + partialTick * 0.15f);
        float ease = 1f - (1f - openProgress) * (1f - openProgress);

        g.fill(0, 0, width, height, ((int) (ease * 0x90)) << 24);
        super.render(g, mouseX, mouseY, partialTick);

        int x = (width - PANEL_W) / 2;
        int y = (int) Mth.lerp(ease, (height - PANEL_H) / 2f + 25, (height - PANEL_H) / 2f);
        int alpha = Math.max(4, (int) (ease * 0xFF));

        g.fill(x, y, x + PANEL_W, y + PANEL_H, ((int) (ease * 0xE0) << 24) | 0x101418);
        g.fill(x, y, x + PANEL_W, y + 18, (alpha << 24) | 0x1E2630);
        g.drawCenteredString(font, "§l" + Key.t("block.advancedweather.auto_sampler"), x + PANEL_W / 2, y + 5, withAlpha(0xFFFFFF, alpha));

        int line = y + 26;
        int left = x + 10;

        String stationLabel = sampler.stationLabel();
        g.drawString(font, Key.t("advancedweather.autosampler.station"), left, line, withAlpha(0xAAAAAA, alpha), true);
        g.drawString(font, stationLabel != null ? "§b" + stationLabel : "§c" + Key.t("advancedweather.autosampler.not_linked"),
                left + 60, line, withAlpha(0xFFFFFF, alpha), true);
        line += 12;

        String archiveLabel = sampler.archiveLabel();
        g.drawString(font, Key.t("advancedweather.autosampler.archive"), left, line, withAlpha(0xAAAAAA, alpha), true);
        g.drawString(font, archiveLabel != null ? "§b" + archiveLabel : "§c" + Key.t("advancedweather.autosampler.not_linked"),
                left + 60, line, withAlpha(0xFFFFFF, alpha), true);
        line += 16;

        g.fill(left, line, x + PANEL_W - 10, line + 1, ((int) (ease * 0x30)) << 24 | 0xFFFFFF);
        line += 8;

        g.drawString(font, Key.t("advancedweather.autosampler.interval"), left, line, withAlpha(0xAAAAAA, alpha), true);
        line += 13;

        hoveredPreset = -1;
        int presetW = 30, presetH = 16, gap = 3;
        int px = left;
        for (int i = 0; i < PRESETS.length; i++) {
            boolean active = sampler.getIntervalTicks() == PRESETS[i].ticks();
            boolean hovered = mouseX >= px && mouseX < px + presetW
                    && mouseY >= line && mouseY < line + presetH;
            if (hovered) hoveredPreset = i;

            int bg = active ? 0x2A4A66 : hovered ? 0x30404C : 0x1A2228;
            g.fill(px, line, px + presetW, line + presetH, (alpha << 24) | bg);
            g.drawCenteredString(font, PRESETS[i].label(), px + presetW / 2, line + 4,
                    withAlpha(active ? 0x7FD4FF : 0xAAAAAA, alpha));
            px += presetW + gap;
        }
        line += presetH + 12;

        var result = sampler.getLastResult();
        String status = switch (result) {
            case IDLE -> "§8" + Key.t("advancedweather.autosampler.status.idle");
            case OK -> "§a" + Key.t("advancedweather.autosampler.status.ok");
            case NO_STATION -> "§c" + Key.t("advancedweather.autosampler.status.no_station");
            case NO_ARCHIVE -> "§c" + Key.t("advancedweather.autosampler.status.no_archive");
            case STATION_NO_SENSORS -> "§6" + Key.t("advancedweather.autosampler.status.no_sensors");
            case ARCHIVE_REJECTED -> "§6" + Key.t("advancedweather.autosampler.status.rejected");
        };
        g.drawString(font, Key.label("advancedweather.autosampler.status") + status, left, line, withAlpha(0xCCCCCC, alpha), true);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && hoveredPreset >= 0) {
            PacketDistributor.sendToServer(new SetSamplerIntervalPacket(
                    sampler.getBlockPos(), PRESETS[hoveredPreset].ticks()));
            Objects.requireNonNull(minecraft).getSoundManager().play(
                    SimpleSoundInstance.forUI(
                            SoundEvents.UI_BUTTON_CLICK.value(), 1.0f));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private static int withAlpha(int rgb, int alpha) {
        return (Mth.clamp(alpha, 0, 255) << 24) | (rgb & 0x00FFFFFF);
    }
}