package net.antopfr.advancedweather.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.antopfr.advancedweather.weather.ServerRain;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PlayerList.class)
public class PlayerListMixin {

    @WrapOperation(
            method = "sendLevelInfo",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;isRaining()Z")
    )
    private boolean advancedweather$keepOurRainToOurPackets(ServerLevel level, Operation<Boolean> original) {
        return ServerRain.of(level) < 0f && original.call(level);
    }
}
