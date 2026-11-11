package net.antopfr.advancedweather.network.toclient;

import net.antopfr.advancedweather.AdvancedWeather;
import net.antopfr.advancedweather.client.event.LightningFlashHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record FlashPacket(float strength) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<FlashPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(AdvancedWeather.MOD_ID, "lightning_flash"));

    public static final StreamCodec<FriendlyByteBuf, FlashPacket> CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, FlashPacket::strength,
            FlashPacket::new
    );

    @Override
    public CustomPacketPayload.@NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(FlashPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> LightningFlashHandler.triggerFlash(packet.strength()));
    }
}
