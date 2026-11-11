package net.antopfr.advancedweather.client.particle.types;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

@OnlyIn(Dist.CLIENT)
public class HailDropParticle extends TextureSheetParticle {

    private static final double MAX_BOUNCE = 0.25D;

    protected HailDropParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z, 0, 0, 0);
        this.lifetime = 40;
        this.quadSize = 0.1F;
        this.alpha = 1.0F;
        this.hasPhysics = true;
        this.pickSprite(sprites);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }

        this.yd -= 0.08D;
        this.move(this.xd, this.yd, this.zd);

        if (this.onGround) {
            this.yd = Math.min(-this.yd * 0.50D, MAX_BOUNCE);
            this.xd *= 0.5D;
            this.zd *= 0.5D;
            if (Math.abs(this.yd) < 0.05D) {
                this.yd = 0;
            }
        }

        this.xd *= 0.99D;
        this.yd *= 0.99D;
        this.zd *= 0.99D;
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(@NotNull SimpleParticleType type, @NotNull ClientLevel level,
                                       double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed) {
            HailDropParticle stone = new HailDropParticle(level, x, y, z, sprites);
            if (xSpeed != 0.0D || ySpeed != 0.0D || zSpeed != 0.0D) stone.setParticleSpeed(xSpeed, ySpeed, zSpeed);
            return stone;
        }
    }
}