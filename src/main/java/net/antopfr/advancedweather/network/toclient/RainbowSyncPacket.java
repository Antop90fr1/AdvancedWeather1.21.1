package net.antopfr.advancedweather.network.toclient;

import net.antopfr.advancedweather.weather.effect.types.rainbows.RainbowRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record RainbowSyncPacket(long start, long end) implements CustomPacketPayload {

    public static final Type<RainbowSyncPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("advancedweather", "rainbow_sync"));

    public static final StreamCodec<FriendlyByteBuf, RainbowSyncPacket> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, RainbowSyncPacket::start,
            ByteBufCodecs.VAR_LONG, RainbowSyncPacket::end,
            RainbowSyncPacket::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RainbowSyncPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> RainbowRenderer.setEvent(packet.start(), packet.end()));
    }
}
