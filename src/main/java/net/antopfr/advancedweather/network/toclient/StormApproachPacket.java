package net.antopfr.advancedweather.network.toclient;

import net.antopfr.advancedweather.client.storm.ComingWeather;
import net.antopfr.advancedweather.weather.WeatherTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record StormApproachPacket(long arrival, WeatherTypes weather) implements CustomPacketPayload {

    public static final long NONE = -1L;

    public static final Type<StormApproachPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("advancedweather", "storm_approach"));

    public static final StreamCodec<FriendlyByteBuf, StormApproachPacket> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, StormApproachPacket::arrival,
            ByteBufCodecs.STRING_UTF8.map(WeatherTypes::fromNameSafe, Enum::name), StormApproachPacket::weather,
            StormApproachPacket::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(StormApproachPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ComingWeather.announce(packet.arrival(), packet.weather()));
    }
}
