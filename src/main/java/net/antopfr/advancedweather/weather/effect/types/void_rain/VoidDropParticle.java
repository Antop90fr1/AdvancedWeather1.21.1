package net.antopfr.advancedweather.weather.effect.types.void_rain;

import net.antopfr.advancedweather.client.particle.types.StreakParticle;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.NotNull;

public class VoidDropParticle extends StreakParticle {

    private static final float STRETCH = 2.2f;
    private static final int CYAN = 0x4DE6FF;
    private static final int VIOLET = 0x9E4FFF;
    private static final float SPLASH_CHANCE = 0.3f;

    protected VoidDropParticle(ClientLevel level, double x, double y, double z,
                               double xSpeed, double zSpeed, SpriteSet sprites) {
        super(level, x, y, z, STRETCH);

        this.xd = xSpeed;
        this.yd = -0.7 - random.nextDouble() * 0.3;
        this.zd = zSpeed;

        this.gravity = 1f;
        this.friction = 0.98f;
        this.lifetime = 100;
        this.hasPhysics = true;
        this.quadSize = 0.2f + random.nextFloat() * 0.08f;
        this.alpha = 0.9f;

        this.pickSprite(sprites);
        int tint = pickTint(random);
        this.setColor((tint >> 16 & 255) / 255f, (tint >> 8 & 255) / 255f, (tint & 255) / 255f);
    }

    public static int pickTint(RandomSource random) {
        return random.nextBoolean() ? CYAN : VIOLET;
    }

    public static void land(ClientLevel level, double x, double y, double z) {
        if (level.random.nextFloat() < SPLASH_CHANCE) {
            level.addParticle(ParticleTypes.REVERSE_PORTAL, x, y + 0.05, z, 0.0, 0.02, 0.0);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.isAlive() || !this.onGround) return;

        land(level, x, y, z);
        this.remove();
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
            return new VoidDropParticle(level, x, y, z, xSpeed, zSpeed, sprites);
        }
    }
}
