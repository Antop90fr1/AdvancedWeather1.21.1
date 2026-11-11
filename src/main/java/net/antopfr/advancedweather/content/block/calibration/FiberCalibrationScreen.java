package net.antopfr.advancedweather.content.block.calibration;

import net.antopfr.advancedweather.AdvancedWeather;
import net.antopfr.advancedweather.util.Key;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public class FiberCalibrationScreen extends CalibrationScreen {

    private static final ResourceLocation BG =
            ResourceLocation.fromNamespaceAndPath(AdvancedWeather.MOD_ID, "textures/gui/calibration_bench_fiber.png");
    private static final ResourceLocation FIBER =
            ResourceLocation.fromNamespaceAndPath(AdvancedWeather.MOD_ID, "textures/gui/fiber.png");

    private static final int FIBER_W = 24;
    private static final int FIBER_TEX_W = 4;
    private static final int FIBER_TEX_H = 30;
    private static final int FIBER_TOP = 8;

    private static final int RAIL_TOP = 24;
    private static final int RAIL_LENGTH = 100;

    private float setpoint = 0.30f;
    private float length = 0.30f;
    private float lengthVel = 0f;

    private final float targetCenter = 0.60f;
    private static final float TARGET_HALF = 0.07f;

    private boolean dragging = false;
    private double lastMouseY = -1;

    public FiberCalibrationScreen(BlockPos benchPos, ItemStack piece, InteractionHand hand) {
        super(benchPos, piece, hand, "advancedweather.calibration.fiber");
    }

    @Override protected ResourceLocation background() {
        return BG;
    }

    @Override
    protected float updateGame(float partialTick, double mouseX, double mouseY) {
        if (dragging && lastMouseY >= 0) {
            float dy = (float) (mouseY - lastMouseY);
            setpoint = Mth.clamp(setpoint + dy * 0.004f, 0f, 1f);
        }
        lastMouseY = mouseY;

        float pull = (setpoint - length) * 0.012f;
        lengthVel += pull * partialTick;
        lengthVel *= 0.93f;
        length = Mth.clamp(length + lengthVel * partialTick, 0f, 1f);

        float dist = Math.abs(length - targetCenter);
        if (dist > TARGET_HALF) return -1f;
        return 1f - (dist / TARGET_HALF);
    }

    @Override
    protected void renderGame(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (dragging && Math.abs(setpoint - length) > 0.01f) {
            float pitch = 0.9f + length * 0.6f;
            tickGestureSound(SoundEvents.WOOL_STEP, pitch, 0.1f, 6);
        }

        int x = panelX(), y = panelY();
        int fiberX = x + 50;
        int railY = y + RAIL_TOP;
        int railCenterX = fiberX + FIBER_W / 2;
        int startY = railY + FIBER_TOP;

        g.fill(railCenterX - 1, railY, railCenterX + 1, railY + FIBER_TOP + RAIL_LENGTH + 12, 0x40000000);

        int targetY = startY + (int) ((targetCenter - TARGET_HALF) * RAIL_LENGTH);
        int targetH = (int) (TARGET_HALF * 2 * RAIL_LENGTH);
        boolean inTarget = Math.abs(length - targetCenter) <= TARGET_HALF;
        int targetCol = inTarget ? 0x6633CC44 : 0x33FFFFFF;
        g.fill(fiberX - 8, targetY, fiberX + FIBER_W + 8, targetY + targetH, targetCol);
        g.fill(fiberX - 8, targetY, fiberX + FIBER_W + 8, targetY + 1, 0xAA33CC44);
        g.fill(fiberX - 8, targetY + targetH, fiberX + FIBER_W + 8, targetY + targetH + 1, 0xAA33CC44);

        int spY = startY + (int) (setpoint * RAIL_LENGTH);
        g.fill(railCenterX - 12, spY, railCenterX + 12, spY + 1, 0xAAFFDD55);
        g.drawString(font, "§e▸", fiberX + FIBER_W + 8, spY - 4, 0xAAFFDD55, false);

        int fiberLen = Math.max(4, (int) (length * RAIL_LENGTH));
        float tautness = length;
        int drawW = Math.round(Mth.lerp(tautness, FIBER_W, FIBER_W * 0.35f));
        int drawX = fiberX + (FIBER_W - drawW) / 2;
        g.blit(FIBER, drawX, startY, drawW, fiberLen, 0, 0, FIBER_TEX_W, FIBER_TEX_H, FIBER_TEX_W, FIBER_TEX_H);
        int tipY = startY + fiberLen;
        int tipCol = inTarget ? 0xFF33CC44 : 0xFFCCCCCC;
        g.fill(drawX, tipY - 1, drawX + drawW, tipY + 1, tipCol);

        g.drawString(font, "§0" + Key.t("advancedweather.calibration.fiber.hint.1"), x + 145, y + 44, 0x88776655, false);
        g.drawString(font, "§0" + Key.t("advancedweather.calibration.fiber.hint.2"), x + 145, y + 56, 0x88776655, false);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) { dragging = true; lastMouseY = my; return true; }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (button == 0) { dragging = false; lastMouseY = -1; return true; }
        return super.mouseReleased(mx, my, button);
    }
}