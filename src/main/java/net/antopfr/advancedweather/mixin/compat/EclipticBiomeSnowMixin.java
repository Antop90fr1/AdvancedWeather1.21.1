package net.antopfr.advancedweather.mixin.compat;

import net.antopfr.advancedweather.compat.eclipticseasons.EclipticSeasonsCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Pseudo
@Mixin(targets = "com.teamtea.eclipticseasons.common.core.biome.WeatherManager$BiomeWeather", remap = false)
public abstract class EclipticBiomeSnowMixin {

    @ModifyVariable(method = "setSnowDepth(F)V", at = @At("HEAD"), argsOnly = true, require = 0)
    private float advancedweather$ourSnow(float theirs) {
        return EclipticSeasonsCompat.snowDepth(this, theirs);
    }
}
