package net.antopfr.advancedweather.network.toclient;

import net.antopfr.advancedweather.weather.wind.WindField;
import net.antopfr.advancedweather.weather.wind.WindState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record WindSyncPacket(float intensity, double driftX, double driftZ, double upperX, double upperZ)
        implements CustomPacketPayload {

    public static final Type<WindSyncPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("advancedweather", "wind_sync"));

    public static final StreamCodec<FriendlyByteBuf, WindSyncPacket> CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, WindSyncPacket::intensity,
            ByteBufCodecs.DOUBLE, WindSyncPacket::driftX,
            ByteBufCodecs.DOUBLE, WindSyncPacket::driftZ,
            ByteBufCodecs.DOUBLE, WindSyncPacket::upperX,
            ByteBufCodecs.DOUBLE, WindSyncPacket::upperZ,
            WindSyncPacket::new
    );

    public static WindSyncPacket of(WindState state) {
        return new WindSyncPacket(state.intensity(),
                state.driftX(1f), state.driftZ(1f), state.upperX(1f), state.upperZ(1f));
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(WindSyncPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> WindField.clientState().applySync(
                packet.intensity(), packet.driftX(), packet.driftZ(), packet.upperX(), packet.upperZ()));
    }
}
