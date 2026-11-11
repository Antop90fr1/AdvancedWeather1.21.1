package net.antopfr.advancedweather.network.toclient;

import net.antopfr.advancedweather.client.state.ClientAtmosphereState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record PressureSyncPacket(float pressure, float trend, float forecast30,
                                 String category, String predictedNext,
                                 String predictedIn30min, String mode,
                                 float confidenceNext, float confidenceIn30)
        implements CustomPacketPayload {

    public static final Type<PressureSyncPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("advancedweather", "pressure_sync"));

    public static final StreamCodec<FriendlyByteBuf, PressureSyncPacket> CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeFloat(packet.pressure());
                buf.writeFloat(packet.trend());
                buf.writeFloat(packet.forecast30());
                buf.writeUtf(packet.category());
                buf.writeUtf(packet.predictedNext());
                buf.writeUtf(packet.predictedIn30min());
                buf.writeUtf(packet.mode());
                buf.writeFloat(packet.confidenceNext());
                buf.writeFloat(packet.confidenceIn30());
            },
            buf -> new PressureSyncPacket(
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readUtf(),
                    buf.readUtf(),
                    buf.readUtf(),
                    buf.readUtf(),
                    buf.readFloat(),
                    buf.readFloat()
            )
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(PressureSyncPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientAtmosphereState.updatePressure(
                packet.pressure(),
                packet.trend(),
                packet.forecast30(),
                packet.category(),
                packet.predictedNext(),
                packet.predictedIn30min(),
                packet.mode(),
                packet.confidenceNext(),
                packet.confidenceIn30()
        ));
    }
}