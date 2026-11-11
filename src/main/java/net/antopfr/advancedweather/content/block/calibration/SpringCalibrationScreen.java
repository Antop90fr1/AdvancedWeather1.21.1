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

public class SpringCalibrationScreen extends CalibrationScreen {

    private static final ResourceLocation SPRING_TOP =
            ResourceLocation.fromNamespaceAndPath(AdvancedWeather.MOD_ID, "textures/gui/spring_top.png");
    private static final ResourceLocation SPRING_COILS =
            ResourceLocation.fromNamespaceAndPath(AdvancedWeather.MOD_ID, "textures/gui/spring_coils.png");
    private static final ResourceLocation SPRING_BOTTOM =
            ResourceLocation.fromNamespaceAndPath(AdvancedWeather.MOD_ID, "textures/gui/spring_bottom.png");

    private static final ResourceLocation BG =
            ResourceLocation.fromNamespaceAndPath(AdvancedWeather.MOD_ID, "textures/gui/calibration_bench_spring.png");

    private static final int SPRING_W = 24;
    private static final int TOP_H = 8;
    private static final int COIL_H = 30;
    private static final int BOTTOM_H = 10;

    private static final int RAIL_TOP = 24;
    private static final int RAIL_LENGTH = 100;

    private float extension = 0.15f;
    private float velocity = 0f;
    private static final float REST = 0.12f;
    private float targetCenter = 0.60f;
    private static final float TARGET_HALF = 0.07f;

    private boolean pulling = false;
    private double lastMouseY = -1;

    public SpringCalibrationScreen(BlockPos benchPos, ItemStack piece, InteractionHand hand) {
        super(benchPos, piece, hand, "advancedweather.calibration.spring");
    }

    @Override protected ResourceLocation background() {
        return BG;
    }

    @Override
    protected float updateGame(float partialTick, double mouseX, double mouseY) {
        float restForce = (REST - extension) * 0.03f;
        velocity += restForce * partialTick;

        if (pulling && lastMouseY >= 0) {
            float dy = (float) (mouseY - lastMouseY);
            velocity += dy * 0.0022f;
        }
        lastMouseY = mouseY;

        velocity *= 0.88f;
        extension = Mth.clamp(extension + velocity * partialTick, 0f, 1f);

        float dist = Math.abs(extension - targetCenter);
        if (dist > TARGET_HALF) return -1f;
        return 1f - (dist / TARGET_HALF);
    }

    @Override
    protected void renderGame(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (pulling) {
            float pitch = 0.6f + extension * 1.2f;
            tickGestureSound(SoundEvents.STONE_BUTTON_CLICK_ON, pitch, 0.25f, 4);
        }

        int x = panelX(), y = panelY();
        int springX = x + 50;
        int railY = y + RAIL_TOP;

        int railCenterX = springX + SPRING_W / 2;
        g.fill(railCenterX - 1, railY, railCenterX + 1, railY + TOP_H + RAIL_LENGTH + BOTTOM_H,
                0x40000000);

        int targetY = railY + TOP_H + (int) ((targetCenter - TARGET_HALF) * RAIL_LENGTH);
        int targetH = (int) (TARGET_HALF * 2 * RAIL_LENGTH);
        boolean inTarget = Math.abs(extension - targetCenter) <= TARGET_HALF;
        int targetCol = inTarget ? 0x6633CC44 : 0x33FFFFFF;
        g.fill(springX - 8, targetY, springX + SPRING_W + 8, targetY + targetH, targetCol);
        g.fill(springX - 8, targetY, springX + SPRING_W + 8, targetY + 1, 0xAA33CC44);
        g.fill(springX - 8, targetY + targetH, springX + SPRING_W + 8, targetY + targetH + 1, 0xAA33CC44);

        int coilsHeight = Math.max(6, (int) (extension * RAIL_LENGTH));

        g.blit(SPRING_TOP, springX, railY, SPRING_W, TOP_H, 0, 0, SPRING_W, TOP_H, SPRING_W, TOP_H);

        int cy = railY + TOP_H;
        g.blit(SPRING_COILS, springX, cy, SPRING_W, coilsHeight, 0, 0, SPRING_W, COIL_H, SPRING_W, COIL_H);

        int hookY = cy + coilsHeight;
        g.blit(SPRING_BOTTOM, springX, hookY, SPRING_W, BOTTOM_H, 0, 0, SPRING_W, BOTTOM_H, SPRING_W, BOTTOM_H);

        g.drawString(font, "§0" + Key.t("advancedweather.calibration.spring.hint.1"), x + 145, y + 44, 0x88776655, false);
        g.drawString(font, "§0" + Key.t("advancedweather.calibration.spring.hint.2"), x + 145, y + 56, 0x88776655, false);
    }

    private boolean overHook(double mx, double my) {
        int x = panelX(), y = panelY();
        int springX = x + 50;
        int railY = y + RAIL_TOP;
        return mx >= springX - 10 && mx <= springX + SPRING_W + 10
                && my >= railY && my <= railY + TOP_H + RAIL_LENGTH + BOTTOM_H + 10;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && overHook(mx, my)) {
            pulling = true;
            lastMouseY = my;
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (button == 0) { pulling = false; lastMouseY = -1; return true; }
        return super.mouseReleased(mx, my, button);
    }
}