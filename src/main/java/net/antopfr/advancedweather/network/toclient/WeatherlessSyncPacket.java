package net.antopfr.advancedweather.network.toclient;

import net.antopfr.advancedweather.client.state.WeatherlessDimension;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record WeatherlessSyncPacket(boolean off) implements CustomPacketPayload {

    public static final Type<WeatherlessSyncPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("advancedweather", "weatherless_sync"));

    public static final StreamCodec<FriendlyByteBuf, WeatherlessSyncPacket> CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, WeatherlessSyncPacket::off,
            WeatherlessSyncPacket::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(WeatherlessSyncPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> WeatherlessDimension.set(packet.off()));
    }
}
