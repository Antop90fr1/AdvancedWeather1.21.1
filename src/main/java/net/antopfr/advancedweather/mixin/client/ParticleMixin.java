package net.antopfr.advancedweather.mixin.client;

import net.antopfr.advancedweather.client.wind.ClientWind;
import net.antopfr.advancedweather.config.AWClientConfig;
import net.antopfr.advancedweather.weather.wind.WindField;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Particle.class)
public abstract class ParticleMixin {

    @Unique
    private static final double advancedweather$COUPLING = 0.03;

    @Shadow @Final protected ClientLevel level;
    @Shadow protected double x;
    @Shadow protected double y;
    @Shadow protected double z;

    @Unique private long advancedweather$pushTick = -1;
    @Unique private Vec3 advancedweather$push = Vec3.ZERO;

    @ModifyVariable(method = "move", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private double advancedweather$windX(double dx) {
        return dx + advancedweather$push().x;
    }

    @ModifyVariable(method = "move", at = @At("HEAD"), argsOnly = true, ordinal = 2)
    private double advancedweather$windZ(double dz) {
        return dz + advancedweather$push().z;
    }

    @Unique
    private Vec3 advancedweather$push() {
        long tick = ClientWind.tick();
        if (advancedweather$pushTick != tick) {
            advancedweather$pushTick = tick;
            advancedweather$push = advancedweather$computePush();
        }
        return advancedweather$push;
    }

    @Unique
    private Vec3 advancedweather$computePush() {
        AWClientConfig config = AWClientConfig.get();
        if (!config.windPushesParticles || ClientWind.intensity() <= 0.01f) return Vec3.ZERO;

        int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(x), Mth.floor(z));
        if (y < surface - 0.5) return Vec3.ZERO;

        return WindField.sample(level, ClientWind.direction(), x, y, z, 0f)
                .velocity()
                .scale(advancedweather$COUPLING * config.windParticleStrength);
    }
}
