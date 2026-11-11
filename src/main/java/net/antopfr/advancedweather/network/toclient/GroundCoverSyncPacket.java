package net.antopfr.advancedweather.network.toclient;

import net.antopfr.advancedweather.client.frost.GroundCover;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record GroundCoverSyncPacket(float snow, float sand, boolean redSand) implements CustomPacketPayload {

    public static final Type<GroundCoverSyncPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("advancedweather", "ground_cover_sync"));

    public static final StreamCodec<FriendlyByteBuf, GroundCoverSyncPacket> CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, GroundCoverSyncPacket::snow,
            ByteBufCodecs.FLOAT, GroundCoverSyncPacket::sand,
            ByteBufCodecs.BOOL, GroundCoverSyncPacket::redSand,
            GroundCoverSyncPacket::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(GroundCoverSyncPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> GroundCover.sync(packet.snow(), packet.sand(), packet.redSand()));
    }
}
