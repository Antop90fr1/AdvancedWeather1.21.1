package net.antopfr.advancedweather.content.item.almanac;

import net.antopfr.advancedweather.AdvancedWeather;
import net.antopfr.advancedweather.content.WeatherRecord;
import net.antopfr.advancedweather.util.Key;
import net.antopfr.advancedweather.util.ValueColors;
import net.antopfr.advancedweather.weather.WeatherTypes;
import net.antopfr.advancedweather.weather.wind.WindScale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public class WeatherAlmanacScreen extends Screen {

    private static final ResourceLocation BOOK_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(AdvancedWeather.MOD_ID, "textures/gui/almanac_book.png");

    private static final ResourceLocation PAGE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(AdvancedWeather.MOD_ID, "textures/gui/book_page.png");

    private static final ResourceLocation COVER_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(AdvancedWeather.MOD_ID, "textures/gui/book_cover.png");

    private static final int BOOK_TEX_W = 414, BOOK_TEX_H = 256;
    private static final int BOOK_W = 414, BOOK_H = 256;

    private static final int SPINE_W = 26;
    private static final int PAGE_W = (BOOK_W - SPINE_W) / 2;
    private static final int PAGE_PAD_X = 18;
    private static final int PAGE_PAD_TOP = 16;
    private static final int PAGE_PAD_BOTTOM = 18;

    private static final int PAGE_TEX_W = 182;
    private static final int PAGE_TEX_H = 232;

    private static final int COVER_W = 211, COVER_H = 254;
    private static final int COVER_RINGS_W = 6; // bande d'anneaux à gauche de la texture

    private static final int LEFT_PAGE_X  = 18;
    private static final int RIGHT_PAGE_X = 214;
    private static final int PAGE_Y = 11;

    private static final int INK_DARK = 0x2E2A3E;
    private static final int INK_SOFT = 0x6B6478;
    private static final int INK_HOVER = 0x7A6AAE;

    private static final int TURN_DURATION_TICKS = 7;

    private static final int OPEN_DURATION_TICKS = 10;

    private int turnTicks = 0;
    private int turnDirection = 0;
    private int displayedSpread = 0;

    private final List<WeatherRecord> records;

    private int spread = 0;

    private enum BookState { CLOSED, OPENING, OPEN }
    private BookState bookState = BookState.CLOSED;

    private int openTicks = 0;

    private float openingProgress(float partialTick) {
        if (bookState != BookState.OPENING) return bookState == BookState.OPEN ? 1f : 0f;
        return Mth.clamp(1f - (openTicks - partialTick) / (float) OPEN_DURATION_TICKS, 0f, 1f);
    }

    private float openingOffsetX(float opening) {
        float closedX = (width - COVER_W) / 2f;
        float openX = bookLeft() + BOOK_W / 2f - COVER_RINGS_W;
        float slide = Mth.clamp(opening * 2f, 0f, 1f);
        slide = 1f - (1f - slide) * (1f - slide);
        return Mth.lerp(slide, closedX - openX, 0f);
    }

    private boolean isOpening() { return openTicks > 0; }

    private Button prevButton, nextButton;

    public WeatherAlmanacScreen(ItemStack almanacStack) {
        super(Component.translatable("item.advancedweather.weather_almanac"));
        this.records = WeatherAlmanacItem.getRecords(almanacStack);
    }

    private int maxSpread() {
        if (records.isEmpty()) return 0;
        return Math.max(0, records.size() / 2);
    }

    private int bookLeft() { return (width - BOOK_W) / 2; }
    private int bookTop()  { return (height - BOOK_H) / 2; }

    private int pageContentX(boolean leftPage) {
        return bookLeft() + (leftPage ? PAGE_PAD_X : PAGE_W + SPINE_W + PAGE_PAD_X);
    }

    private int pageContentWidth() {
        return PAGE_W - PAGE_PAD_X * 2;
    }

    private boolean openSoundPlayed = false;

    @Override
    protected void init() {
        super.init();
        prevButton = addRenderableWidget(Button.builder(Component.literal("<"), b -> turnPage(-1))
                .bounds(bookLeft() - 26, bookTop() + BOOK_H / 2 - 10, 20, 20).build());
        nextButton = addRenderableWidget(Button.builder(Component.literal(">"), b -> turnPage(1))
                .bounds(bookLeft() + BOOK_W + 6, bookTop() + BOOK_H / 2 - 10, 20, 20).build());
        if (!openSoundPlayed) {
            Objects.requireNonNull(minecraft).getSoundManager().play(
                    SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 0.8f));
            openSoundPlayed = true;
        }
        updateButtons();
    }

    private float turnProgress(float partialTick) {
        if (turnTicks <= 0) return 1f;
        float raw = 1f - (turnTicks - partialTick) / (float) TURN_DURATION_TICKS;
        return Mth.clamp(raw, 0f, 1f);
    }

    private void turnPage(int direction) {
        if (isOpening() || turnTicks > 0) return;
        int target = Mth.clamp(spread + direction, 0, maxSpread());
        if (target != spread) {
            displayedSpread = spread;
            spread = target;
            turnDirection = direction;
            turnTicks = TURN_DURATION_TICKS;
            Objects.requireNonNull(minecraft).getSoundManager().play(
                    SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0f));
            updateButtons();
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (bookState == BookState.OPENING && openTicks > 0) {
            openTicks--;
            if (openTicks <= 0) {
                bookState = BookState.OPEN;
                updateButtons();
            }
        } else if (bookState == BookState.OPEN && turnTicks > 0) {
            turnTicks--;
        }
    }

    private void updateButtons() {
        boolean open = bookState == BookState.OPEN;
        prevButton.visible = open;
        nextButton.visible = open;
        prevButton.active = open && spread > 0;
        nextButton.active = open && spread < maxSpread();
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderTransparentBackground(g); // comme BookViewScreen — pas de blur
    }

    @Override
    public void render(@NotNull GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);

        int bx = bookLeft();
        int by = bookTop();

        float opening = openingProgress(partialTick);

        if (bookState == BookState.CLOSED) {
            int cx = (width - COVER_W) / 2;   // centré écran
            int cy = bookTop() + (BOOK_H - COVER_H) / 2;
            g.blit(COVER_TEXTURE, cx, cy, 0, 0, COVER_W, COVER_H, COVER_W, COVER_H);

            float pulse = 0.6f + 0.4f * Mth.sin((Objects.requireNonNull(minecraft).gui.getGuiTicks() + partialTick) * 0.15f);
            int hintAlpha = (int) (pulse * 255);
            String hint = Key.t("advancedweather.almanac.click_to_open");
            g.drawString(font, hint, cx + COVER_W / 2 - font.width(hint) / 2,
                    cy + COVER_H + 6, withAlpha(0xFFFFFF, hintAlpha), true);
            return;
        }

        if (bookState == BookState.OPENING) {
            float offsetX = openingOffsetX(opening);

            g.pose().pushPose();
            g.pose().translate(offsetX, 0, 0);

            if (opening < 0.5f) {
                float fold = 1f - opening * 2f;

                int coverEdge = bx + BOOK_W / 2;
                g.enableScissor((int) (coverEdge - (1f - fold) * (BOOK_W / 2f) + offsetX), by,
                        (int) (bx + BOOK_W + offsetX), by + BOOK_H);
                g.blit(BOOK_TEXTURE, bx, by, 0, 0, BOOK_W, BOOK_H, BOOK_TEX_W, BOOK_TEX_H);
                if (!records.isEmpty()) renderSpread(g, -1, -1, spread, false, true);
                g.disableScissor();

                renderCoverFold(g, fold);
            } else {
                g.blit(BOOK_TEXTURE, bx, by, 0, 0, BOOK_W, BOOK_H, BOOK_TEX_W, BOOK_TEX_H);
                if (!records.isEmpty()) {
                    renderSpread(g, -1, -1, spread, false, true);
                    float unfold = (opening - 0.5f) * 2f;
                    renderFoldedPage(g, true, spread, unfold);
                }
            }

            g.pose().popPose();
            return;
        }

        g.blit(BOOK_TEXTURE, bx, by, 0, 0, BOOK_W, BOOK_H, BOOK_TEX_W, BOOK_TEX_H);

        if (records.isEmpty()) {
            int px = pageContentX(true);
            int py = by + PAGE_PAD_TOP;

            g.drawString(font, Key.c("§o", "advancedweather.almanac.empty_screen"), px + 2, py + TOC_TITLE_Y, INK_SOFT, false);

            return;
        }

        float progress = turnProgress(partialTick);

        if (turnTicks <= 0 || progress >= 1f) {
            renderSpread(g, mouseX, mouseY, spread, true, true);
        } else if (turnDirection > 0) {
            renderSpread(g, mouseX, mouseY, displayedSpread, true, false);
            renderSpread(g, mouseX, mouseY, spread, false, true);

            if (progress < 0.5f) {
                float fold = 1f - progress * 2f;
                renderFoldedPage(g, false, displayedSpread, fold);
            } else {
                float unfold = (progress - 0.5f) * 2f;
                renderFoldedPage(g, true, spread, unfold);
            }
        } else {
            renderSpread(g, mouseX, mouseY, displayedSpread, false, true);
            renderSpread(g, mouseX, mouseY, spread, true, false);

            if (progress < 0.5f) {
                float fold = 1f - progress * 2f;
                renderFoldedPage(g, true, displayedSpread, fold * fold);
            } else {
                float unfold = (progress - 0.5f) * 2f;
                renderFoldedPage(g, false, spread, 1-((1-unfold)*(1-unfold)));
            }
        }

        String pageLabel = (spread + 1) + " / " + (maxSpread() + 1);
        g.drawString(font, pageLabel,
                bx + BOOK_W / 2 - font.width(pageLabel) / 2,
                by + BOOK_H + 4, 0xCCCCCC, true);
    }

    private void renderSpread(GuiGraphics g, int mouseX, int mouseY, int whichSpread,
                              boolean left, boolean right) {
        if (whichSpread == 0) {
            if (left) renderTableOfContents(g, turnTicks > 0 ? -1 : mouseX, turnTicks > 0 ? -1 : mouseY);
            if (right) renderRecordPage(g, false, 0);
        } else {
            if (left) renderRecordPage(g, true, whichSpread * 2 - 1);
            if (right) {
                int idx = whichSpread * 2;
                if (idx < records.size()) renderRecordPage(g, false, idx);
            }
        }
    }

    private void renderFoldedPage(GuiGraphics g, boolean leftPage, int whichSpread, float fold) {
        if (fold <= 0.02f) return;

        int px = bookLeft() + (leftPage ? LEFT_PAGE_X : RIGHT_PAGE_X);

        int pivotX = leftPage ? px + PAGE_TEX_W : px;

        g.pose().pushPose();
        g.pose().translate(pivotX, 0, 0);
        g.pose().scale(fold, 1f, 1f);
        g.pose().translate(-pivotX, 0, 0);

        int py = bookTop() + PAGE_Y;

        g.blit(PAGE_TEXTURE, px, py, 0, 0, PAGE_TEX_W, PAGE_TEX_H, PAGE_TEX_W, PAGE_TEX_H);
        renderSpread(g, -1, -1, whichSpread, leftPage, !leftPage);

        int shade = (int) ((1f - fold) * 90f);
        g.fill(px, bookTop() + 6, px + py, bookTop() + BOOK_H - 6, (shade << 24));

        g.pose().popPose();
    }

    private static final int TOC_TITLE_Y = 0;
    private static final int TOC_START_Y = 16;
    private static final int TOC_LINE_H = 11;
    private static final int TOC_MAX_LINES = 17;

    private void renderTableOfContents(GuiGraphics g, int mouseX, int mouseY) {
        int px = pageContentX(true);
        int py = bookTop() + PAGE_PAD_TOP;
        int contentW = pageContentWidth();

        g.drawString(font, Key.t("advancedweather.almanac.contents"), px + 2, py + TOC_TITLE_Y, INK_DARK, false);

        int shown = Math.min(records.size(), TOC_MAX_LINES);
        for (int i = 0; i < shown; i++) {
            WeatherRecord r = records.get(i);
            int lineY = py + TOC_START_Y + i * TOC_LINE_H;

            boolean hovered = mouseX >= px && mouseX < px + contentW
                    && mouseY >= lineY - 1 && mouseY < lineY + TOC_LINE_H - 1;

            long day = r.gameTime() / 24000L;
            int hours = (int) ((r.gameTime() + 6000) / 1000 % 24);
            int minutes = (int) ((r.gameTime() % 1000) * 60 / 1000);
            String entry = Component.translatable("advancedweather.time.day_clock", day,
                    String.format("%02d:%02d", hours, minutes)).getString() + " - " + r.weatherType().displayString();
            if (font.width(entry) > contentW - 12) {
                entry = font.plainSubstrByWidth(entry, contentW - 18) + "…";
            }
            g.drawString(font, entry, px + 2, lineY, hovered ? INK_HOVER : 0x4A4460, false);
            if (hovered) {
                g.drawString(font, "→", px + contentW - 8, lineY, INK_HOVER, false);
            }
        }
        if (records.size() > TOC_MAX_LINES) {
            g.drawString(font, "§o+" + (records.size() - TOC_MAX_LINES) + " more…",
                    px + 2, py + TOC_START_Y + shown * TOC_LINE_H, INK_SOFT, false);
        }
    }

    private void renderCoverFold(GuiGraphics g, float fold) {
        if (fold <= 0.02f) return;

        int px = bookLeft() + BOOK_W / 2 - COVER_RINGS_W;
        int py = bookTop() + (BOOK_H - COVER_H) / 2;
        int pivotX = px + COVER_RINGS_W;

        g.pose().pushPose();
        g.pose().translate(pivotX, 0, 0);
        g.pose().scale(fold, 1f, 1f);
        g.pose().translate(-pivotX, 0, 0);

        g.blit(COVER_TEXTURE, px, py, 0, 0, COVER_W, COVER_H, COVER_W, COVER_H);

        int shade = (int) ((1f - fold) * 60f);
        g.fill(px, py, px + COVER_W, py + COVER_H, (shade << 24));

        g.pose().popPose();
    }

    private void renderRecordPage(GuiGraphics g, boolean leftPage, int recordIndex) {
        if (recordIndex < 0 || recordIndex >= records.size()) return;
        WeatherRecord r = records.get(recordIndex);
        int px = pageContentX(leftPage);
        int py = bookTop() + PAGE_PAD_TOP;
        int contentW = pageContentWidth();
        int centerX = px + contentW / 2;

        long day = r.gameTime() / 24000L;
        int hours = (int) ((r.gameTime() + 6000) / 1000 % 24);
        int minutes = (int) ((r.gameTime() % 1000) * 60 / 1000);
        drawCentered(g, Component.translatable("advancedweather.time.day_clock", day,
                        String.format("%02d:%02d", hours, minutes)).getString(),
                centerX, py + 2, INK_DARK);

        drawWeatherIcon(g, r.weatherType(), centerX - 10, py + 18, 20);
        drawCentered(g, r.weatherType().displayString(), centerX, py + 44, INK_DARK);

        g.fill(px + 16, py + 58, px + contentW - 16, py + 59, 0x30000000);

        int line = py + 70;
        int labelX = px + 8;
        drawValueLine(g, labelX, line, Key.t("advancedweather.temp"),
                String.format("%.1f °C", r.temperature()),
                ValueColors.temperature(r.temperature())); line += 15;

        drawValueLine(g, labelX, line, Key.t("advancedweather.pressure"),
                String.format("%.1f hPa", r.pressure()), INK_DARK); line += 15;

        drawValueLine(g, labelX, line, Key.t("advancedweather.humidity"),
                String.format("%.0f%% [%s]", r.humidity(), ValueColors.humidityLabel(r.humidity())),
                ValueColors.humidity(r.humidity())); line += 15;

        float kmh = WindScale.intensityToKmh(r.windIntensity());
        drawValueLine(g, labelX, line, Key.t("advancedweather.wind"),
                String.format("%.1f km/h", kmh),
                ValueColors.wind(kmh)); line += 13;

        drawCentered(g, "[" + WindScale.beaufort(kmh).getString() + "]",
                centerX, line, INK_SOFT);

        String origin = r.stationName().isBlank() ? r.biome().getPath()
                + " @"
                + r.pos().getX() + ", "
                +  r.pos().getY() + ", "
                + r.pos().getZ() : r.stationName();

        if (font.width(origin) > contentW - 8) {
            origin = font.plainSubstrByWidth(origin, contentW - 14) + "…";
        }
        drawCentered(g, "§o" + origin, centerX,
                bookTop() + BOOK_H - PAGE_PAD_BOTTOM - 8, INK_SOFT);
    }

    private void drawCentered(GuiGraphics g, String text, int centerX, int y, int color) {
        String stripped = ChatFormatting.stripFormatting(text);
        int w = font.width(stripped != null ? stripped : text);
        g.drawString(font, text, centerX - w / 2, y, color, false);
    }

    private void drawValueLine(GuiGraphics g, int x, int y, String label, String value, int valueColor) {
        g.drawString(font, label, x, y, INK_SOFT, false);
        g.drawString(font, value, x + 60, y, valueColor, false);
    }

    private void drawWeatherIcon(GuiGraphics g, WeatherTypes type, int x, int y, int size) {
        ResourceLocation icon = ResourceLocation.fromNamespaceAndPath(AdvancedWeather.MOD_ID,
                "textures/gui/weather_icons/" + type.name().toLowerCase() + ".png");
        g.blit(icon, x, y, size, size, 0f, 0f, 32, 32, 32, 32);
    }

    private void openBook() {
        if (bookState != BookState.CLOSED) return;
        bookState = BookState.OPENING;
        openTicks = OPEN_DURATION_TICKS;
        Objects.requireNonNull(minecraft).getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 0.8f));
        updateButtons();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (bookState == BookState.CLOSED && button == 0) {
            int cx = (width - COVER_W) / 2;
            int cy = bookTop() + (BOOK_H - COVER_H) / 2;
            if (mouseX >= cx && mouseX < cx + COVER_W && mouseY >= cy && mouseY < cy + COVER_H) {
                openBook();
                return true;
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }
        if (bookState != BookState.OPEN) return super.mouseClicked(mouseX, mouseY, button);
        if (button == 0 && spread == 0 && !records.isEmpty()) {
            int px = pageContentX(true);
            int py = bookTop() + PAGE_PAD_TOP;
            int contentW = pageContentWidth();
            int shown = Math.min(records.size(), TOC_MAX_LINES);
            for (int i = 0; i < shown; i++) {
                int lineY = py + TOC_START_Y + i * TOC_LINE_H;
                if (mouseX >= px && mouseX < px + contentW
                        && mouseY >= lineY - 1 && mouseY < lineY + TOC_LINE_H - 1) {
                    spread = (i + 1) / 2;
                    updateButtons();
                    Objects.requireNonNull(minecraft).getSoundManager().play(
                            SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0f));
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (bookState == BookState.CLOSED && (keyCode == 257 || keyCode == 32)) { // Enter / Space
            openBook();
            return true;
        }
        if (bookState != BookState.OPEN) return super.keyPressed(keyCode, scanCode, modifiers);
        if (keyCode == 262) { turnPage(1); return true; }   // →
        if (keyCode == 263) { turnPage(-1); return true; }  // ←
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static int withAlpha(int rgb, int alpha) {
        return (Mth.clamp(alpha, 0, 255) << 24) | (rgb & 0x00FFFFFF);
    }
}