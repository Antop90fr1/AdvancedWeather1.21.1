package net.antopfr.advancedweather.content.item.kite;

import net.antopfr.advancedweather.content.entity.KiteEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.WeakHashMap;

public final class KiteHold {

    private KiteHold() {}

    private static final double SHOULDER_SIDE = 0.31;
    private static final double SHOULDER_HEIGHT = 1.3;
    private static final double SHOULDER_HEIGHT_CROUCHED = 1.0;
    private static final double ARM_LENGTH = 0.6;

    private static final float MIN_PITCH = -0.3f;
    private static final float MAX_PITCH = 1.4f;
    private static final float MAX_YAW_DEGREES = 100f;

    private static final float TOWED_REACH = -Mth.PI - 0.35f;
    private static final float TOWED_SPREAD = 0.2f;
    private static final double TOWED_HAND_AHEAD = 1.85;
    private static final double TOWED_HAND_RISE = 0.3;

    private static final Map<Player, KiteEntity> KITES = new WeakHashMap<>();

    private record Aim(float yaw, float pitch, Vec3 hand) {}

    public static KiteEntity kiteOf(Player player) {
        KiteEntity known = KITES.get(player);
        if (known != null && known.isAlive() && known.getOwnerPlayer() == player) return known;

        KiteEntity found = null;
        for (KiteEntity kite : player.level().getEntitiesOfClass(KiteEntity.class, player.getBoundingBox().inflate(24.0))) {
            if (kite.getOwnerPlayer() == player) {
                found = kite;
                break;
            }
        }
        KITES.put(player, found);
        return found;
    }

    static void pose(HumanoidModel<?> model, LivingEntity entity, HumanoidArm arm) {
        boolean right = arm == HumanoidArm.RIGHT;
        ModelPart holding = right ? model.rightArm : model.leftArm;
        ModelPart other = right ? model.leftArm : model.rightArm;
        float inward = right ? 1f : -1f;

        if (entity.isFallFlying()) {
            holding.xRot = TOWED_REACH;
            holding.yRot = 0f;
            holding.zRot = TOWED_SPREAD * inward;
            other.xRot = TOWED_REACH;
            other.yRot = 0f;
            other.zRot = -TOWED_SPREAD * inward;
            return;
        }

        KiteEntity kite = entity instanceof Player player ? kiteOf(player) : null;
        if (kite == null) {
            holding.xRot = -2.2f;
            holding.yRot = 0f;
            holding.zRot = 0.1f * inward;
            return;
        }

        float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
        Aim aim = aim((Player) entity, arm, kite.getPosition(partialTick), partialTick);
        holding.xRot = -Mth.HALF_PI - aim.pitch();
        holding.yRot = aim.yaw();
        holding.zRot = 0f;
    }

    public static Vec3 handPosition(Player player, KiteEntity kite, float partialTick) {
        if (player.isFallFlying()) {
            return player.getPosition(partialTick)
                    .add(player.getViewVector(partialTick).scale(TOWED_HAND_AHEAD))
                    .add(0.0, TOWED_HAND_RISE, 0.0);
        }
        return aim(player, holdingArm(player), kite.getPosition(partialTick), partialTick).hand();
    }

    private static HumanoidArm holdingArm(Player player) {
        return player.getMainHandItem().getItem() instanceof KiteItem
                ? player.getMainArm()
                : player.getMainArm().getOpposite();
    }

    private static Aim aim(Player player, HumanoidArm arm, Vec3 kite, float partialTick) {
        float bodyYaw = Mth.lerp(partialTick, player.yBodyRotO, player.yBodyRot);
        double bodyYawRad = Math.toRadians(bodyYaw);
        double side = arm == HumanoidArm.RIGHT ? -SHOULDER_SIDE : SHOULDER_SIDE;
        Vec3 shoulder = player.getPosition(partialTick).add(
                Math.cos(bodyYawRad) * side,
                player.isCrouching() ? SHOULDER_HEIGHT_CROUCHED : SHOULDER_HEIGHT,
                Math.sin(bodyYawRad) * side);

        Vec3 to = kite.subtract(shoulder);
        double flat = Math.sqrt(to.x * to.x + to.z * to.z);
        float pitch = Mth.clamp((float) Math.atan2(to.y, flat), MIN_PITCH, MAX_PITCH);
        float yawTo = (float) Math.toDegrees(Math.atan2(to.z, to.x)) - 90f;
        float yaw = Mth.clamp(Mth.wrapDegrees(yawTo - bodyYaw), -MAX_YAW_DEGREES, MAX_YAW_DEGREES);

        double worldYaw = Math.toRadians(bodyYaw + yaw);
        Vec3 direction = new Vec3(
                -Math.sin(worldYaw) * Math.cos(pitch),
                Math.sin(pitch),
                Math.cos(worldYaw) * Math.cos(pitch));
        return new Aim(yaw * Mth.DEG_TO_RAD, pitch, shoulder.add(direction.scale(ARM_LENGTH)));
    }
}
