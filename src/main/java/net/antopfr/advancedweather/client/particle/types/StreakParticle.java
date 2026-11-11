package net.antopfr.advancedweather.client.particle.types;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

public abstract class StreakParticle extends TextureSheetParticle {

    private static final float MIN_LENGTH_SQUARED = 1.0E-6f;

    private final float stretch;

    protected StreakParticle(ClientLevel level, double x, double y, double z, float stretch) {
        super(level, x, y, z);
        this.stretch = stretch;
    }

    @Override
    public void render(@NotNull VertexConsumer buffer, @NotNull Camera camera, float partialTicks) {
        Vec3 eye = camera.getPosition();
        float x = (float) (Mth.lerp(partialTicks, this.xo, this.x) - eye.x);
        float y = (float) (Mth.lerp(partialTicks, this.yo, this.y) - eye.y);
        float z = (float) (Mth.lerp(partialTicks, this.zo, this.z) - eye.z);

        Vector3f down = streakAxis();
        Vector3f side = new Vector3f(down).cross(x, y, z);
        if (side.lengthSquared() < MIN_LENGTH_SQUARED) side.set(down).cross(1f, 0f, 0f);

        float halfWidth = this.getQuadSize(partialTicks);
        side.normalize(halfWidth);
        down.mul(halfWidth * stretch);
        int light = this.getLightColor(partialTicks);

        vertex(buffer, x + side.x + down.x, y + side.y + down.y, z + side.z + down.z, this.getU1(), this.getV1(), light);
        vertex(buffer, x + side.x - down.x, y + side.y - down.y, z + side.z - down.z, this.getU1(), this.getV0(), light);
        vertex(buffer, x - side.x - down.x, y - side.y - down.y, z - side.z - down.z, this.getU0(), this.getV0(), light);
        vertex(buffer, x - side.x + down.x, y - side.y + down.y, z - side.z + down.z, this.getU0(), this.getV1(), light);
    }

    private Vector3f streakAxis() {
        Vector3f axis = new Vector3f((float) xd, (float) yd, (float) zd);
        if (axis.lengthSquared() < MIN_LENGTH_SQUARED) return axis.set(0f, -1f, 0f);

        axis.normalize();
        return axis.y > 0f ? axis.negate() : axis;
    }

    private void vertex(VertexConsumer buffer, float x, float y, float z, float u, float v, int light) {
        buffer.addVertex(x, y, z)
                .setUv(u, v)
                .setColor(this.rCol, this.gCol, this.bCol, this.alpha)
                .setLight(light);
    }

    @Override
    protected int getLightColor(float partialTick) {
        return LightTexture.FULL_BRIGHT;
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }
}
