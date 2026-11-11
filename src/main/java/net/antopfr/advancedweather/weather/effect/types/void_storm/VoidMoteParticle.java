package net.antopfr.advancedweather.weather.effect.types.void_storm;

import net.antopfr.advancedweather.client.particle.types.StreakParticle;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

public class VoidMoteParticle extends StreakParticle {

    private static final float OPACITY = 0.85f;
    private static final int FADE_IN_TICKS = 6;
    private static final int FADE_OUT_TICKS = 12;

    protected VoidMoteParticle(ClientLevel level, double x, double y, double z, double ySpeed, SpriteSet sprites) {
        super(level, x, y, z, 1f);
        this.pickSprite(sprites);

        float pale = random.nextFloat() * 0.35f;
        this.rCol = 0.55f + pale;
        this.gCol = 0.30f + pale;
        this.bCol = 0.95f;
        this.alpha = 0f;

        this.xd = (random.nextDouble() - 0.5) * 0.02;
        this.yd = ySpeed;
        this.zd = (random.nextDouble() - 0.5) * 0.02;

        this.gravity = 0f;
        this.friction = 0.985f;
        this.lifetime = 34 + random.nextInt(20);
        this.hasPhysics = true;
        this.quadSize = 0.12f + random.nextFloat() * 0.08f;
    }

    @Override
    public void tick() {
        double before = this.y;
        super.tick();
        if (!this.isAlive()) return;

        if (this.y - before < 0.01) {
            this.remove();
            return;
        }
        float in = this.age / (float) FADE_IN_TICKS;
        float out = (this.lifetime - this.age) / (float) FADE_OUT_TICKS;
        this.alpha = OPACITY * Mth.clamp(Math.min(in, out), 0f, 1f);
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
            return new VoidMoteParticle(level, x, y, z, ySpeed, sprites);
        }
    }
}
