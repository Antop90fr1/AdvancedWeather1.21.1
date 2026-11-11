package net.antopfr.advancedweather.content.item;

import net.antopfr.advancedweather.content.entity.SeedingRocketEntity;
import net.antopfr.advancedweather.weather.AtmosphericForcing;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class SeedingRocketItem extends Item {

    private final AtmosphericForcing.Bias bias;
    private final float pressurePush;
    private final float dewPointPush;
    private final float tempOffset;
    private final int forcingTicks;

    public SeedingRocketItem(Properties props, AtmosphericForcing.Bias bias,
                             float pressurePush, float dewPointPush, float tempOffset, int forcingTicks) {
        super(props);
        this.bias = bias;
        this.pressurePush = pressurePush;
        this.dewPointPush = dewPointPush;
        this.tempOffset = tempOffset;
        this.forcingTicks = forcingTicks;
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();

        if (!level.isClientSide) {
            ItemStack stack = context.getItemInHand();
            Vec3 click = context.getClickLocation();
            Direction face = context.getClickedFace();

            SeedingRocketEntity rocket = new SeedingRocketEntity(
                    level,
                    click.x + face.getStepX() * 0.15,
                    click.y + face.getStepY() * 0.15,
                    click.z + face.getStepZ() * 0.15,
                    bias, pressurePush, dewPointPush, tempOffset, forcingTicks);

            level.addFreshEntity(rocket);

            Player player = context.getPlayer();
            if (player == null || !player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}