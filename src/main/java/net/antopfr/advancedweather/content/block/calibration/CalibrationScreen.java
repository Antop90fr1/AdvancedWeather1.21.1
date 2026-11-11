package net.antopfr.advancedweather.content.block.calibration;

import net.antopfr.advancedweather.network.toserver.CommitCalibrationPacket;
import net.antopfr.advancedweather.util.Key;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public abstract class CalibrationScreen extends Screen {

    protected static final int PANEL_W = 256;
    protected static final int PANEL_H = 166;

    protected abstract ResourceLocation background();

    protected final BlockPos benchPos;
    protected final ItemStack piece;
    protected final InteractionHand hand;

    protected float progress = 0f;

    protected boolean committed = false;

    private boolean wasInTarget = false;
    private int lastProgressStep = -1;

    private int gestureSoundCooldown = 0;

    protected CalibrationScreen(BlockPos benchPos, ItemStack piece, InteractionHand hand, String titleKey) {
        super(Component.translatable(titleKey));
        this.benchPos = benchPos;
        this.piece = piece;
        this.hand = hand;
    }

    protected int panelX() { return (width - PANEL_W) / 2; }
    protected int panelY() { return (height - PANEL_H) / 2; }

    protected abstract float updateGame(float partialTick, double mouseX, double mouseY);

    protected abstract void renderGame(GuiGraphics g, int mouseX, int mouseY, float partialTick);

    @Override
    public void render(@NotNull GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int x = panelX(), y = panelY();

        g.fill(0, 0, width, height, 0x90000000);
        g.blit(background(), x, y, 0, 0, PANEL_W, PANEL_H, PANEL_W, PANEL_H);
        g.fill(x, y, x + PANEL_W, y + 16, 0xFF1E2630);
        g.drawCenteredString(font, title, x + PANEL_W / 2, y + 4, 0xFFFFFF);

        if (!committed) {
            float justesse = updateGame(partialTick, mouseX, mouseY);
            boolean inTarget = justesse >= 0f;

            if (inTarget && !wasInTarget) {
                playSound(SoundEvents.NOTE_BLOCK_CHIME.value(), 1.6f, 0.5f);
            }
            else if (!inTarget && wasInTarget) {
                playSound(SoundEvents.NOTE_BLOCK_HAT.value(), 0.8f, 0.3f);
            }
            wasInTarget = inTarget;

            if (inTarget) {
                progress = Math.min(1f, progress + justesse * 0.006f * partialTick);
            } else {
                progress = Math.max(0f, progress - 0.002f * partialTick);
            }

            int step = (int) (progress * 4);
            if (step > lastProgressStep && step > 0) {
                lastProgressStep = step;
                float pitch = 0.8f + step * 0.25f;
                playSound(SoundEvents.NOTE_BLOCK_PLING.value(), pitch, 0.4f);
            }

            if (progress >= 1f) commit();
        }

        renderGame(g, mouseX, mouseY, partialTick);

        int barX = x + 20, barY = y + PANEL_H - 22, barW = PANEL_W - 40;
        g.fill(barX, barY, barX + barW, barY + 8, 0xFF000000);
        int fill = (int) (barW * progress);
        int col = progress >= 1f ? 0xFF33CC44 : 0xFF67CFDB;
        g.fill(barX, barY, barX + fill, barY + 8, col);
        g.drawCenteredString(font, "§7" + Key.t("advancedweather.calibration.title"), x + PANEL_W / 2, barY - 12, 0xAAAAAA);
    }

    private void commit() {
        committed = true;
        PacketDistributor.sendToServer(new CommitCalibrationPacket(benchPos, hand));
        Objects.requireNonNull(minecraft).setScreen(null);
    }

    @Override public boolean isPauseScreen() { return false; }

    protected void playSound(SoundEvent sound, float pitch, float volume) {
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(sound, pitch, volume));
    }

    protected void tickGestureSound(SoundEvent sound, float pitch, float volume, int intervalTicks) {
        if (gestureSoundCooldown > 0) {
            gestureSoundCooldown--;
            return;
        }
        gestureSoundCooldown = intervalTicks;
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(sound, pitch, volume));
    }
}
