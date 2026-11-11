package net.antopfr.advancedweather.weather.effect.types.chorus_petals;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

public class ChorusPetalParticle extends TextureSheetParticle {

    private static final float OPACITY = 0.9f;
    private static final int FADE_TICKS = 15;

    private float spin;

    protected ChorusPetalParticle(ClientLevel level, double x, double y, double z,
                                  double xSpeed, double ySpeed, double zSpeed, SpriteSet sprites) {
        super(level, x, y, z);
        this.pickSprite(sprites);

        float shade = 0.75f + random.nextFloat() * 0.25f;
        this.rCol = 0.80f * shade;
        this.gCol = 0.62f * shade;
        this.bCol = 0.95f * shade;
        this.alpha = OPACITY;

        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;

        this.gravity = 0.02f;
        this.friction = 0.99f;
        this.lifetime = 70 + random.nextInt(50);
        this.hasPhysics = true;
        this.quadSize = 0.07f + random.nextFloat() * 0.06f;

        this.roll = random.nextFloat() * Mth.TWO_PI;
        this.oRoll = this.roll;
        this.spin = (random.nextFloat() - 0.5f) * 0.6f;
    }

    @Override
    public void tick() {
        this.oRoll = this.roll;
        super.tick();

        if (this.onGround) this.spin = 0f;
        this.roll += this.spin;
        this.alpha = OPACITY * Mth.clamp((this.lifetime - this.age) / (float) FADE_TICKS, 0f, 1f);
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
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
            return new ChorusPetalParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, sprites);
        }
    }
}
