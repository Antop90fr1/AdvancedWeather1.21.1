package net.antopfr.advancedweather.content.entity;

import net.antopfr.advancedweather.content.AWDataComponents;
import net.antopfr.advancedweather.content.advancement.AWAdvancements;
import net.antopfr.advancedweather.content.item.kite.KiteColors;
import net.antopfr.advancedweather.content.item.kite.KiteItem;
import net.antopfr.advancedweather.weather.wind.WindField;
import net.antopfr.advancedweather.weather.wind.WindSample;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class KiteEntity extends Entity {

    private static final EntityDataAccessor<Integer> DATA_OWNER =
            SynchedEntityData.defineId(KiteEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<KiteColors> DATA_COLORS =
            SynchedEntityData.defineId(KiteEntity.class, AWEntitySerializers.KITE_COLORS_SERIALIZER.get());

    private static final int BURN_TICKS = 30;

    private int burnTicks;

    private static final double FLIGHT_REACH = 6.0;
    private static final double FLIGHT_RISE = 4.2;
    private static final double FLIGHT_STEER = 0.7;
    private static final double TRAIL_REACH = 5.0;
    private static final double TRAIL_RISE = 2.0;
    private static final double MAX_LINE = 9.0;
    private static final double FLIGHT_THRUST = 0.05;
    private static final double FLIGHT_LIFT = 0.035;
    private static final double MAX_CLIMB = 0.4;

    private Vec3 towOffset;

    public KiteEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public KiteEntity(Level level, Player owner, KiteColors colors) {
        this(AWEntities.KITE.get(), level);
        setPos(owner.getX(), owner.getY() + 3.5, owner.getZ());
        entityData.set(DATA_OWNER, owner.getId());
        entityData.set(DATA_COLORS, colors);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        b.define(DATA_OWNER, -1);
        b.define(DATA_COLORS, KiteColors.WHITE);
    }

    public KiteColors getColors() { return entityData.get(DATA_COLORS); }

    private Player owner() {
        int id = entityData.get(DATA_OWNER);
        return id >= 0 && level().getEntity(id) instanceof Player p ? p : null;
    }

    @Override
    public void tick() {
        super.tick();
        Player owner = owner();
        if (owner == null || !holdingKite(owner)) {
            if (!level().isClientSide) discard();
            return;
        }
        if (level() instanceof ServerLevel server && tickBurning(server, owner)) return;

        WindSample wind = WindField.sample(level(), position());
        float intensity = wind.intensity();
        Vec3 windDir = wind.direction();

        float bob = Mth.sin((tickCount + intensity * 20f) * 0.15f) * 0.15f;

        if (owner.isFallFlying()) {
            if (towOffset == null) towOffset = position().subtract(owner.position());
            towOffset = towOffset.add(flightOffset(owner, wind).subtract(towOffset).scale(0.2));
            if (towOffset.lengthSqr() > MAX_LINE * MAX_LINE) towOffset = towOffset.normalize().scale(MAX_LINE);

            Vec3 next = owner.position().add(towOffset);
            setPos(next.x, next.y + bob * 0.5f, next.z);
        } else {
            towOffset = null;

            double height = 3.5 + intensity * 2.0;
            double drift = 2.0 + intensity * 3.0;
            Vec3 target = owner.position().add(0, height, 0).add(windDir.scale(drift));

            Vec3 pos = position();
            Vec3 next = pos.add(target.subtract(pos).scale(0.15));
            setPos(next.x, next.y + bob, next.z);
        }

        if (level().isClientSide) {
            if (owner.isLocalPlayer()) applyPull(owner, wind);
        } else if (owner.fallDistance > 0 && intensity > 0.45f) {
            owner.fallDistance = Math.min(owner.fallDistance, 3.0f);
        }
    }

    private static Vec3 flightOffset(Player owner, WindSample wind) {
        Vec3 look = owner.getLookAngle();
        double flat = Math.sqrt(look.x * look.x + look.z * look.z);
        Vec3 heading = flat > 1.0e-3 ? new Vec3(look.x / flat, 0, look.z / flat) : wind.direction();

        Vec3 steered = wind.direction().add(heading.scale(FLIGHT_STEER));
        double length = Math.sqrt(steered.x * steered.x + steered.z * steered.z);
        Vec3 ahead = new Vec3(steered.x / length * FLIGHT_REACH, FLIGHT_RISE, steered.z / length * FLIGHT_REACH);

        Vec3 behind = new Vec3(-heading.x * TRAIL_REACH, TRAIL_RISE, -heading.z * TRAIL_REACH);
        return behind.lerp(ahead, lead(wind.intensity()));
    }

    private static double lead(float intensity) {
        return Mth.clamp((intensity - 0.1f) / 0.3f, 0f, 1f);
    }

    private boolean tickBurning(ServerLevel level, Player owner) {
        if (!isOnFire() && touchesFire()) igniteForSeconds(8f);
        if (!isOnFire()) {
            burnTicks = 0;
            return false;
        }

        if (++burnTicks < BURN_TICKS) {
            if (burnTicks % 3 == 0) {
                level.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 0.5, getZ(), 2, 0.3, 0.3, 0.3, 0.01);
            }
            return false;
        }

        burnUp(level, owner);
        return true;
    }

    private boolean touchesFire() {
        return level().getBlockStatesIfLoaded(getBoundingBox().deflate(1.0E-6))
                .anyMatch(state -> state.is(BlockTags.FIRE));
    }

    private void burnUp(ServerLevel level, Player owner) {
        level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.5, getZ(), 12, 0.35, 0.35, 0.35, 0.02);
        level.sendParticles(ParticleTypes.FLAME, getX(), getY() + 0.5, getZ(), 8, 0.3, 0.3, 0.3, 0.02);
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.NEUTRAL, 0.7f, 1.4f);

        ItemStack burnt = deployedStack(owner);
        if (!burnt.isEmpty()) {
            burnt.remove(AWDataComponents.KITE_DEPLOYED.get());
            if (!owner.hasInfiniteMaterials()) burnt.shrink(1);
        }
        discard();
    }

    private static ItemStack deployedStack(Player owner) {
        ItemStack fallback = ItemStack.EMPTY;
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = owner.getItemInHand(hand);
            if (!(stack.getItem() instanceof KiteItem)) continue;
            if (stack.getOrDefault(AWDataComponents.KITE_DEPLOYED.get(), false)) return stack;
            if (fallback.isEmpty()) fallback = stack;
        }
        return fallback;
    }

    @Override
    public void thunderHit(@NotNull ServerLevel level, @NotNull LightningBolt lightning) {
        super.thunderHit(level, lightning);
        if (owner() instanceof ServerPlayer player) AWAdvancements.grant(player, "franklin");
    }

    private boolean holdingKite(Player p) {
        return p.getMainHandItem().getItem() instanceof KiteItem
                || p.getOffhandItem().getItem() instanceof KiteItem;
    }

    private void applyPull(Player owner, WindSample air) {
        float intensity = air.intensity();
        Vec3 motion = owner.getDeltaMovement();

        if (owner.isFallFlying()) {
            Vec3 line = position().subtract(owner.position());
            double flat = Math.sqrt(line.x * line.x + line.z * line.z);
            double lead = lead(intensity);
            if (flat < 1.0e-3 || lead <= 0.0) return;

            double towardsX = line.x / flat;
            double towardsZ = line.z / flat;
            double topSpeed = 0.5 + 1.5 * intensity;
            double along = motion.x * towardsX + motion.z * towardsZ;
            double thrust = FLIGHT_THRUST * intensity * lead * Mth.clamp(1.0 - along / topSpeed, 0.0, 1.0);
            double lift = motion.y < MAX_CLIMB ? FLIGHT_LIFT * intensity * lead : 0.0;

            owner.setDeltaMovement(motion.x + towardsX * thrust, motion.y + lift, motion.z + towardsZ * thrust);
            return;
        }

        if (intensity < 0.45f) return;

        float pull = Math.min(1f, (intensity - 0.45f) / 0.55f);
        Vec3 wind = air.direction();
        double drift = 0.035 * pull;
        double lift = !owner.onGround() && motion.y > 0 ? 0.045 * pull : 0.0;

        owner.setDeltaMovement(motion.x + wind.x * drift, motion.y + lift, motion.z + wind.z * drift);
    }

    public Player getOwnerPlayer() { return owner(); }

    @Override protected void readAdditionalSaveData(@NotNull CompoundTag tag) {}
    @Override protected void addAdditionalSaveData(@NotNull CompoundTag tag) {}
}
