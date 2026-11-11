package net.antopfr.advancedweather.weather.effect.types.lava_rain;

import net.antopfr.advancedweather.client.particle.types.StreakParticle;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.NotNull;

public class LavaDropParticle extends StreakParticle {

    private static final float STRETCH = 2.2f;

    protected LavaDropParticle(ClientLevel level, double x, double y, double z,
                               double xSpeed, double zSpeed, SpriteSet sprites) {
        super(level, x, y, z, STRETCH);
        this.pickSprite(sprites);

        this.xd = xSpeed;
        this.yd = -0.6 - random.nextDouble() * 0.3;
        this.zd = zSpeed;

        this.gravity = 1.2f;
        this.friction = 0.98f;
        this.lifetime = 100;
        this.hasPhysics = true;
        this.quadSize = 0.22f + random.nextFloat() * 0.1f;
        this.alpha = 0.95f;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.isAlive()) return;

        if (this.onGround || level.getFluidState(BlockPos.containing(x, y, z)).is(FluidTags.LAVA)) {
            land(level, x, y, z);
            this.remove();
        }
    }

    public static void land(ClientLevel level, double x, double y, double z) {
        if (!level.getFluidState(BlockPos.containing(x, y - 0.05, z)).is(FluidTags.LAVA)) {
            RandomSource random = level.random;
            level.addParticle(ParticleTypes.LANDING_LAVA, x, y + 0.05, z, 0.0, 0.0, 0.0);
            if (random.nextFloat() < 0.25f) level.addParticle(ParticleTypes.SMOKE, x, y + 0.1, z, 0.0, 0.02, 0.0);
            if (random.nextFloat() < 0.1f) level.addParticle(ParticleTypes.LAVA, x, y + 0.1, z, 0.0, 0.0, 0.0);
        }
        LavaRainHandler.impact(level, x, y, z);
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(@NotNull SimpleParticleType type, @NotNull ClientLevel level,
                                       double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed) {
            return new LavaDropParticle(level, x, y, z, xSpeed, zSpeed, sprites);
        }
    }
}
