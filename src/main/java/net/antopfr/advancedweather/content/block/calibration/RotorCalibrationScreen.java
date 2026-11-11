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

public class RotorCalibrationScreen extends CalibrationScreen {

    private static final ResourceLocation BG =
            ResourceLocation.fromNamespaceAndPath(AdvancedWeather.MOD_ID, "textures/gui/calibration_bench_rotor.png");
    private static final ResourceLocation ROTOR =
            ResourceLocation.fromNamespaceAndPath(AdvancedWeather.MOD_ID, "textures/gui/rotor.png");

    private static final int ROTOR_SIZE = 64;
    private static final int DIAL_CX_OFFSET = 62;
    private static final int DIAL_CY_OFFSET = 74;

    private float angle = 0f;
    private float speed = 0f;
    private static final float TARGET_SPEED = 9f;
    private static final float TARGET_HALF = 4.8f;

    private boolean dragging = false;
    private float lastPointerAngle = Float.NaN;

    public RotorCalibrationScreen(BlockPos benchPos, ItemStack piece, InteractionHand hand) {
        super(benchPos, piece, hand, "advancedweather.calibration.rotor");
    }

    @Override protected ResourceLocation background() {
        return BG;
    }

    private int dialCx() { return panelX() + DIAL_CX_OFFSET; }
    private int dialCy() { return panelY() + DIAL_CY_OFFSET; }

    private float pointerAngle(double mouseX, double mouseY) {
        double dx = mouseX - dialCx();
        double dy = mouseY - dialCy();
        return (float) Math.toDegrees(Math.atan2(dy, dx));
    }

    @Override
    protected float updateGame(float partialTick, double mouseX, double mouseY) {

        if (dragging) {
            float pa = pointerAngle(mouseX, mouseY);
            if (!Float.isNaN(lastPointerAngle)) {
                float delta = Mth.degreesDifference(lastPointerAngle, pa);
                speed += delta * 0.04f;
            }
            lastPointerAngle = pa;
        } else {
            lastPointerAngle = Float.NaN;
        }

        speed *= 0.975f;
        speed = Mth.clamp(speed, -30f, 30f);

        angle = (angle + speed) % 360f;

        float dist = Math.abs(Math.abs(speed) - TARGET_SPEED);
        if (dist > TARGET_HALF) return -1f;
        return 1f - (dist / TARGET_HALF);
    }

    @Override
    protected void renderGame(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (dragging && Math.abs(speed) > 0.5f) {
            float speedNorm = Mth.clamp(Math.abs(speed) / 15f, 0f, 1f);
            float pitch = 0.7f + speedNorm;
            int interval = Math.max(2, 8 - (int)(speedNorm * 6));
            tickGestureSound(SoundEvents.NOTE_BLOCK_BASS.value(), pitch, 0.2f, interval);
        }

        int cx = dialCx(), cy = dialCy();
        int x = panelX(), y = panelY();

        int gaugeX = cx - 40, gaugeY = cy + 42, gaugeW = 80, gaugeH = 6;
        g.fill(gaugeX, gaugeY, gaugeX + gaugeW, gaugeY + gaugeH, 0x60000000);

        float maxDisplay = 18f;
        int tLo = gaugeX + (int) ((TARGET_SPEED - TARGET_HALF) / maxDisplay * gaugeW);
        int tHi = gaugeX + (int) ((TARGET_SPEED + TARGET_HALF) / maxDisplay * gaugeW);
        g.fill(tLo, gaugeY, tHi, gaugeY + gaugeH, 0x8033CC44);

        int spX = gaugeX + (int) (Mth.clamp(Math.abs(speed) / maxDisplay, 0f, 1f) * gaugeW);
        boolean inTarget = Math.abs(Math.abs(speed) - TARGET_SPEED) <= TARGET_HALF;
        g.fill(spX - 1, gaugeY - 2, spX + 1, gaugeY + gaugeH + 2, inTarget ? 0xFF33CC44 : 0xFFD9A036);

        g.pose().pushPose();
        g.pose().translate(cx, cy, 0);
        g.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(angle));
        g.pose().translate(-ROTOR_SIZE / 2f, -ROTOR_SIZE / 2f, 0);
        g.blit(ROTOR, 0, 0, ROTOR_SIZE, ROTOR_SIZE, 0, 0, ROTOR_SIZE, ROTOR_SIZE, ROTOR_SIZE, ROTOR_SIZE);
        g.pose().popPose();

        g.drawString(font, "§0" + Key.t("advancedweather.calibration.rotor.hint.1"), x + 145, y + 44, 0x88776655, false);
        g.drawString(font, "§0" + Key.t("advancedweather.calibration.rotor.hint.2"), x + 145, y + 56, 0x88776655, false);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            dragging = true;
            lastPointerAngle = pointerAngle(mx, my);
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (button == 0) { dragging = false; lastPointerAngle = Float.NaN; return true; }
        return super.mouseReleased(mx, my, button);
    }
}
