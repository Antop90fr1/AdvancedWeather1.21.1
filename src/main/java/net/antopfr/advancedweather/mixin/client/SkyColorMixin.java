package net.antopfr.advancedweather.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.antopfr.advancedweather.client.render.SkyVeil;
import net.antopfr.advancedweather.compat.atmospherics.AtmosphericsCompat;
import net.antopfr.advancedweather.util.SkyMixinContext;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = ClientLevel.class, priority = 1100)
public class SkyColorMixin {

    @WrapMethod(method = "getSkyColor")
    private Vec3 aw_tintSkyColor(Vec3 cameraPos, float partialTick, Operation<Vec3> original) {
        Vec3 vanilla;
        AtmosphericsCompat.beginSkyColor();
        try {
            vanilla = original.call(cameraPos, partialTick);
        } finally {
            AtmosphericsCompat.endSkyColor();
        }
        SkyMixinContext.setVanillaSkyColor(vanilla);
        return SkyVeil.tint(vanilla, partialTick);
    }
}
