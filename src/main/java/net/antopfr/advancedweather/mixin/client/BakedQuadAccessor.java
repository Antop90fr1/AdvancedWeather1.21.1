package net.antopfr.advancedweather.mixin.client;

import net.minecraft.client.renderer.block.model.BakedQuad;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BakedQuad.class)
public interface BakedQuadAccessor {

    @Mutable
    @Accessor("vertices")
    void advancedweather$setVertices(int[] vertices);
}
