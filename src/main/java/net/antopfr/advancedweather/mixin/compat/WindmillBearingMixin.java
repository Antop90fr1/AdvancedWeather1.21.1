package net.antopfr.advancedweather.mixin.compat;

import com.simibubi.create.content.contraptions.bearing.MechanicalBearingBlockEntity;
import com.simibubi.create.content.contraptions.bearing.WindmillBearingBlockEntity;
import net.antopfr.advancedweather.compat.create.WindmillDirectionFactor;
import net.antopfr.advancedweather.config.AWCommonConfig;
import net.antopfr.advancedweather.weather.wind.WindField;
import net.antopfr.advancedweather.weather.wind.WindSample;
import net.antopfr.advancedweather.weather.wind.WindScale;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MechanicalBearingBlockEntity.class)
@OnlyIn(Dist.CLIENT)
public abstract class WindmillBearingMixin {

    @Unique
    private float aw_smoothedWindIntensity = 0f;

    @Inject(method = "getAngularSpeed", at = @At("RETURN"), cancellable = true)
    private void aw_applyVisualWindBonus(CallbackInfoReturnable<Float> cir) {
        Object self = this;
        if (!(self instanceof WindmillBearingBlockEntity)) return;

        MechanicalBearingBlockEntity bearingSelf = (MechanicalBearingBlockEntity) self;
        Level level = bearingSelf.getLevel();
        if (level == null || !level.isClientSide()) return;

        AWCommonConfig config = AWCommonConfig.get();
        if (!config.enableCreateWindmillCompat) return;

        WindSample air = WindField.sample(level, bearingSelf.getBlockPos());
        aw_smoothedWindIntensity = Mth.lerp(0.02f, aw_smoothedWindIntensity, air.intensity());

        float threshold = (float) config.windmillWindThreshold;
        if (aw_smoothedWindIntensity <= threshold) return;

        float windKmh = WindScale.intensityToKmh(aw_smoothedWindIntensity);
        float windScaleFactor = (float) config.windmillSpeedScaleFactor;
        float speedMultiplier = 1.0f + (windKmh * windScaleFactor);

        if (config.windmillDirectionAffectsOutput) {
            BlockState state = bearingSelf.getBlockState();
            if (state.hasProperty(BlockStateProperties.FACING)) {
                Direction facing = state.getValue(BlockStateProperties.FACING);
                Vec3 windDir = air.direction();
                float directionFactor = WindmillDirectionFactor.compute(facing, windDir);
                float bonusPortion = speedMultiplier - 1.0f;
                speedMultiplier = 1.0f + bonusPortion * directionFactor;
            }
        }

        float pureGeneratedSpeed = bearingSelf instanceof WindmillBearingBlockEntity windmill
                ? windmill.getGeneratedSpeed()
                : 0f;
        float pureAngular = pureGeneratedSpeed * 360f / 60f / 20f;

        cir.setReturnValue(pureAngular * speedMultiplier);
    }
}