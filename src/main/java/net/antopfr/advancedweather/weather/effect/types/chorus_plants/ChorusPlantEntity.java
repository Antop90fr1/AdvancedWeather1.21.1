package net.antopfr.advancedweather.weather.effect.types.chorus_plants;

import net.antopfr.advancedweather.weather.WeatherEffects;
import net.antopfr.advancedweather.weather.effect.EffectManager;
import net.antopfr.advancedweather.weather.wind.WindField;
import net.antopfr.advancedweather.weather.wind.WindSample;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class ChorusPlantEntity extends Entity {

    private static final EntityDataAccessor<Integer> HEALTH =
            SynchedEntityData.defineId(ChorusPlantEntity.class, EntityDataSerializers.INT);

    private float windSpeed;
    private float scale;

    public ChorusPlantEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = false;
        this.windSpeed = 0.08f + (float) Math.random() * 0.07f;
        this.scale = 1.0f; // taille bloc normale
        this.refreshDimensions();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(HEALTH, 2);
    }

    @Override
    public void tick() {
        super.tick();

        if (!level().isClientSide) {
            if (tickCount > 600 && !EffectManager.get((ServerLevel) level()).hasEffect(WeatherEffects.CHORUS_PLANTS)) {
                this.discard();
                return;
            }

            if (level().getNearestPlayer(this, 80) == null) {
                this.discard();
                return;
            }
        }

        WindSample air = WindField.sample(level(), position());
        Vec3 wind = air.direction();
        float speed = (float) (0.05 + air.blocksPerTick() * 0.30) * (windSpeed / 0.115f);

        this.setDeltaMovement(
                wind.x * speed,
                this.getDeltaMovement().y - 0.035,
                wind.z * speed
        );

        if (this.onGround()) {
            this.setDeltaMovement(
                    wind.x * speed,
                    0.22 + Math.random() * 0.18,
                    wind.z * speed
            );
        }

        this.move(MoverType.SELF, this.getDeltaMovement());
        this.setDeltaMovement(getDeltaMovement().multiply(0.997, 0.997, 0.997));
    }

    @Override
    public boolean isPickable() {
        return !this.isRemoved();
    }

    @Override
    public boolean isAttackable() {
        return true;
    }

    @Override
    public boolean skipAttackInteraction(@NotNull Entity attacker) {
        return this.hurt(damageSources().playerAttack((Player) attacker), 1.0f);
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        if (level().isClientSide) return false;

        int hp = entityData.get(HEALTH) - 1;
        if (hp <= 0) {
            this.level().playSound(
                    null,
                    this.getX(), this.getY(), this.getZ(),
                    SoundEvents.AZALEA_BREAK,
                    SoundSource.AMBIENT,
                    1.0F,
                    0.9F
            );

            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(
                        new BlockParticleOption(
                                ParticleTypes.BLOCK,
                                Blocks.CHORUS_PLANT.defaultBlockState()
                        ),
                        this.getX(), this.getY() + 0.5, this.getZ(),
                        25,
                        0.4, 0.4, 0.4,
                        0.06
                );
            }

            this.spawnAtLocation(Items.CHORUS_FRUIT);
            this.discard();
        } else {
            entityData.set(HEALTH, hp);

            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.CHORUS_FLOWER_GROW, SoundSource.AMBIENT, 0.5F, 1.1F);

            Entity attacker = source.getEntity();

            double pushX = (Math.random() - 0.5) * 0.2;
            double pushZ = (Math.random() - 0.5) * 0.2;
            double pushY = 0.32;

            if (attacker != null) {
                double dirX = this.getX() - attacker.getX();
                double dirZ = this.getZ() - attacker.getZ();

                double distance = Math.sqrt(dirX * dirX + dirZ * dirZ);
                if (distance > 0.001) {
                    dirX /= distance;
                    dirZ /= distance;
                }

                pushX = dirX * 0.55;
                pushZ = dirZ * 0.55;
            }

            this.setDeltaMovement(
                    this.getDeltaMovement().x + pushX,
                    pushY,
                    this.getDeltaMovement().z + pushZ
            );

            this.hasImpulse = true;
        }
        return true;
    }

    @Override
    public boolean isPushable() {
        return true;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.windSpeed = tag.getFloat("windSpeed");
        this.scale = tag.contains("scale") ? tag.getFloat("scale") : 1.8f;
        if (tag.contains("health")) {
            entityData.set(HEALTH, tag.getInt("health"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("windSpeed", windSpeed);
        tag.putFloat("scale", scale);
        tag.putInt("health", entityData.get(HEALTH));
    }

    public float getVisualRotation(float partialTick) {
        return (tickCount + partialTick) * 12f;
    }

    public float getScale() {
        return scale;
    }
}
