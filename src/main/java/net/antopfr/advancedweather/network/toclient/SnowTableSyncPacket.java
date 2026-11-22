package net.antopfr.advancedweather.network.toclient;

import net.antopfr.advancedweather.client.frost.GroundCover;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public record SnowTableSyncPacket(Map<ResourceLocation, Float> snow) implements CustomPacketPayload {

    public static final Type<SnowTableSyncPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("advancedweather", "snow_table_sync"));

    public static final StreamCodec<FriendlyByteBuf, SnowTableSyncPacket> CODEC = StreamCodec.composite(
            ByteBufCodecs.map(HashMap::new, ResourceLocation.STREAM_CODEC, ByteBufCodecs.FLOAT), SnowTableSyncPacket::snow,
            SnowTableSyncPacket::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SnowTableSyncPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> GroundCover.syncTable(packet.snow()));
    }
}
