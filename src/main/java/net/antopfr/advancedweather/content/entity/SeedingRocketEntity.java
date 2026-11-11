package net.antopfr.advancedweather.content.entity;

import net.antopfr.advancedweather.client.render.SeedingBurst;
import net.antopfr.advancedweather.content.item.AWItems;
import net.antopfr.advancedweather.weather.AtmosphericForcing;
import net.antopfr.advancedweather.weather.WeatherManager;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class SeedingRocketEntity extends Projectile {

    private static final EntityDataAccessor<Integer> DATA_BIAS =
            SynchedEntityData.defineId(SeedingRocketEntity.class, EntityDataSerializers.INT);

    private static final int DEFAULT_FORCING_TICKS = 2400;

    private int life;
    private int lifetime;

    private float pressurePush = -18f;
    private float dewPointPush = 8f;
    private float tempOffset = 0f;
    private int forcingTicks = DEFAULT_FORCING_TICKS;

    public SeedingRocketEntity(EntityType<? extends SeedingRocketEntity> type, Level level) {
        super(type, level);
    }

    public SeedingRocketEntity(Level level, double x, double y, double z,
                               AtmosphericForcing.Bias bias,
                               float pressurePush, float dewPointPush, float tempOffset, int forcingTicks) {
        this(AWEntities.SEEDING_ROCKET.get(), level);
        this.life = 0;
        setPos(x, y, z);
        this.entityData.set(DATA_BIAS, bias.ordinal());
        this.pressurePush = pressurePush;
        this.dewPointPush = dewPointPush;
        this.tempOffset = tempOffset;
        this.forcingTicks = forcingTicks;

        this.setDeltaMovement(this.random.triangle(0.0, 0.002297), 0.05,
                this.random.triangle(0.0, 0.002297));

        int flightDuration = 3;
        this.lifetime = 10 * flightDuration + this.random.nextInt(6) + this.random.nextInt(7);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_BIAS, AtmosphericForcing.Bias.SEEDING.ordinal());
    }

    public AtmosphericForcing.Bias getBias() {
        return AtmosphericForcing.Bias.values()[this.entityData.get(DATA_BIAS)];
    }

    public ItemStack getItem() {
        return new ItemStack(switch (getBias()) {
            case SEEDING     -> AWItems.SEEDING_ROCKET_RAIN.get();
            case DISSIPATING -> AWItems.SEEDING_ROCKET_CLEAR.get();
            case COOLING     -> AWItems.SEEDING_ROCKET_FROST.get();
            case HEATING     -> AWItems.SEEDING_ROCKET_WARM.get();
            case NONE        -> AWItems.SEEDING_ROCKET_RAIN.get();
        });
    }

    @Override
    public void tick() {
        super.tick();

        double accel = this.horizontalCollision ? 1.0 : 1.15;
        setDeltaMovement(getDeltaMovement().multiply(accel, 1.0, accel).add(0.0, 0.04, 0.0));

        Vec3 move = getDeltaMovement();
        move(MoverType.SELF, move);
        setDeltaMovement(move);
        updateRotation();

        if (this.life == 0 && !isSilent()) {
            level().playSound(null, getX(), getY(), getZ(),
                    SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.AMBIENT, 3f, 1f);
        }

        if (level().isClientSide && this.life % 2 == 0) {
            level().addParticle(ParticleTypes.FIREWORK,
                    getX(), getY(), getZ(),
                    this.random.nextGaussian() * 0.05,
                    -getDeltaMovement().y * 0.5,
                    this.random.nextGaussian() * 0.05);
        }

        this.life++;
        if (!level().isClientSide && this.life > this.lifetime) {
            explode();
        }
    }

    private void explode() {
        ServerLevel serverLevel = (ServerLevel) level();

        WeatherManager.get(serverLevel).seed(
                serverLevel, getBias(), pressurePush, dewPointPush, tempOffset, forcingTicks);

        serverLevel.playSound(null, getX(), getY(), getZ(),
                SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.AMBIENT, 3f, 0.8f);
        serverLevel.playSound(null, getX(), getY(), getZ(),
                SoundEvents.GLASS_BREAK, SoundSource.AMBIENT, 1.5f, 1.2f);

        level().broadcastEntityEvent(this, (byte) 17);
        discard();
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 17 && level().isClientSide) {
            SeedingBurst.spawn(level(), position(), getBias());
        }
        super.handleEntityEvent(id);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 4096.0;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.life = tag.getInt("Life");
        this.lifetime = tag.getInt("LifeTime");
        this.pressurePush = tag.getFloat("pPush");
        this.dewPointPush = tag.getFloat("dpPush");
        this.tempOffset = tag.getFloat("tempOff");
        this.forcingTicks = tag.contains("forcingTicks") ? tag.getInt("forcingTicks") : DEFAULT_FORCING_TICKS;
        this.entityData.set(DATA_BIAS, tag.getInt("bias"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Life", this.life);
        tag.putInt("LifeTime", this.lifetime);
        tag.putFloat("pPush", this.pressurePush);
        tag.putFloat("dpPush", this.dewPointPush);
        tag.putFloat("tempOff", this.tempOffset);
        tag.putInt("forcingTicks", this.forcingTicks);
        tag.putInt("bias", this.entityData.get(DATA_BIAS));
    }
}
